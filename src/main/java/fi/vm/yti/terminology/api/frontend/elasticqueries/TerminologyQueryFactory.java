package fi.vm.yti.terminology.api.frontend.elasticqueries;

import java.util.*;
import java.util.regex.Pattern;

import fi.vm.yti.terminology.api.exception.InvalidQueryException;
import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.frontend.TerminologyType;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.terminology.api.frontend.searchdto.DeepSearchHitListDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.InformationDomainDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.OrganizationDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologyDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchRequest;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchResponse;
import fi.vm.yti.terminology.api.util.ElasticRequestUtils;

public class TerminologyQueryFactory {

    private static final Logger log = LoggerFactory.getLogger(TerminologyQueryFactory.class);

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_PAGE_FROM = 0;

    private ObjectMapper objectMapper;

    public TerminologyQueryFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SearchRequest createQuery(TerminologySearchRequest request,
                                     boolean superUser,
                                     Set<String> privilegedOrganizations) {
        return createQuery(
                request,
                Collections.emptySet(),
                superUser,
                privilegedOrganizations);
    }

    public SearchRequest createQuery(TerminologySearchRequest request,
                                     Collection<String> additionalTerminologyIds,
                                     boolean superUser,
                                     Set<String> privilegedOrganizations) {
        return createQuery(
                request.getQuery(),
                request.getStatuses(),
                request.getGroups(),
                request.getTypes(),
                request.getOrganizations(),
                additionalTerminologyIds,
                pageSize(request),
                pageFrom(request),
                superUser,
                privilegedOrganizations);
    }

    private SearchRequest createQuery(String query,
                                      String[] statuses,
                                      String[] groupIds,
                                      String[] types,
                                      String[] organizationIds,
                                      Collection<String> additionalTerminologyIds,
                                      int pageSize,
                                      int pageFrom,
                                      boolean superUser,
                                      Set<String> privilegedOrganizations) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .from(pageFrom)
            .size(pageSize);

        QueryBuilder incompleteQuery = statusAndContributorQuery(privilegedOrganizations);

        // collect all queries in these, and later create a bool query if
        // there are more than one
        var mustQueries = new ArrayList<QueryBuilder>();
        var shouldQueries = new ArrayList<QueryBuilder>();

        if (!query.isEmpty()) {
            var labelQuery = ElasticRequestUtils
                    .buildPrefixSuffixQuery(query)
                    .fields(Map.of(
                            "properties.prefLabel.value", 1.0f,
                            "references.contributor.properties.prefLabel.value", 2.0f));
            mustQueries.add(labelQuery);
        }

        if (statuses != null && statuses.length > 0) {
            mustQueries.add(ElasticRequestUtils.buildStatusQuery(
                    statuses, "properties.status.value"));
        }

        if (groupIds != null && groupIds.length > 0)  {
            try {
                Arrays.stream(groupIds).forEach(x -> UUID.fromString(x));
            } catch (IllegalArgumentException exception){
                log.error("One or more group IDs were invalid");
                throw new InvalidQueryException("One or more group IDs were invalid");
            }

            mustQueries.add(QueryBuilders.termsQuery(
                    "references.inGroup.id", groupIds));
        }

        if (organizationIds != null && organizationIds.length > 0)  {
            try {
                Arrays.stream(organizationIds).forEach(x -> UUID.fromString(x));
            } catch (IllegalArgumentException exception){
                log.error("One or more organization IDs were invalid");
                throw new InvalidQueryException("One or organization IDs were invalid");
            }

            mustQueries.add(QueryBuilders.termsQuery(
                    "references.contributor.id", organizationIds));
        }

        QueryBuilder typeQuery = null;
        if (types != null && types.length > 0)  {
            final var validTypes = new String[] {
                    TerminologyType.TERMINOLOGICAL_VOCABULARY.name(),
                    TerminologyType.OTHER_VOCABULARY.name()
            };
            if (!Arrays.asList(validTypes).containsAll(Arrays.asList(types))) {
                log.error("One or more vocabulary types were invalid");
                throw new InvalidQueryException("One or more vocabulary types were invalid");
            }
            // vocabulary type query must also be applied later when
            // filtering by additionalTerminologyIds
            typeQuery = QueryBuilders.termsQuery("terminologyType", types);
            mustQueries.add(typeQuery);
        }

        // if the search was also done to concepts, we may have
        // extra terminologies here
        if (additionalTerminologyIds != null && !additionalTerminologyIds.isEmpty()) {
            var idQuery = QueryBuilders.termsQuery(
                    "type.graph.id.keyword",
                    additionalTerminologyIds);

            QueryBuilder boolIdQuery = null;
            if (typeQuery != null) {
                shouldQueries.add(QueryBuilders.boolQuery()
                    .must(idQuery)
                    .must(typeQuery));
            } else {
                shouldQueries.add(idQuery);
            }
        }

        QueryBuilder mustQuery = null;
        if (mustQueries.size() > 1) {
            // several queries, collect them into a bool query
            mustQuery = QueryBuilders.boolQuery();
            for (var boolableQuery : mustQueries) {
                mustQuery = ((BoolQueryBuilder) mustQuery).must(boolableQuery);
            }
        } else if (mustQueries.size() == 1) {
            mustQuery = mustQueries.get(0);
        } else {
            // no specific queries, search all
            mustQuery = QueryBuilders.matchAllQuery();
        }
        mustQuery = combineIncompleteQuery(mustQuery, incompleteQuery, superUser);

        // now we've created the must (AND) query, need to wrap it in another
        // boolean OR if we have been provided with terminologyIds
        QueryBuilder shouldQuery = null;
        if (!shouldQueries.isEmpty()) {
            shouldQueries.add(mustQuery);
            shouldQuery = QueryBuilders.boolQuery();
            for (var boolableQuery : shouldQueries) {
                shouldQuery = ((BoolQueryBuilder) shouldQuery).should(boolableQuery);
            }
            ((BoolQueryBuilder) shouldQuery).minimumShouldMatch(1);
        }

        if (statuses == null || statuses.length == 0) {
            QueryBuilder boostingQuery = QueryBuilders.boostingQuery(
                    shouldQuery != null ? shouldQuery : mustQuery,
                    QueryBuilders
                            .boolQuery()
                            .must(QueryBuilders.matchQuery("properties.status.value", Status.SUPERSEDED.name()))
            ).negativeBoost(0.1f);

            sourceBuilder.query(boostingQuery);
        } else {
            sourceBuilder.query(shouldQuery != null ? shouldQuery : mustQuery);
        }

        SearchRequest sr = new SearchRequest("vocabularies")
            .source(sourceBuilder);
        log.debug("Terminology Query request: " + sr.toString());
        return sr;
    }

    public SearchRequest createMatchingTerminologiesQuery(Set<String> privilegedOrganizations) {
        return createMatchingTerminologiesQuery(privilegedOrganizations, null);
    }

    public SearchRequest createMatchingTerminologiesQuery(final Collection<String> privilegedOrganizations,
                                                          final Collection<String> limitToThese) {
        // TODO: When terminology node ID starts to be "the" id then fetchSource(false) and modify parsing, and change id limit to terms query.
        final QueryBuilder contribQuery = QueryBuilders.termsQuery("references.contributor.id", privilegedOrganizations);
        QueryBuilder finalQuery = contribQuery;
        if (limitToThese != null && !limitToThese.isEmpty()) {
            // QueryBuilders.termsQuery("id", limitToThese);
            final BoolQueryBuilder limitQuery = QueryBuilders.boolQuery().minimumShouldMatch(1);
            for (String id : limitToThese) {
                limitQuery.should(QueryBuilders.matchQuery("type.graph.id", id));
            }
            finalQuery = QueryBuilders.boolQuery()
                .must(contribQuery)
                .must(limitQuery);
        }

        SearchRequest sr = new SearchRequest("vocabularies")
            .source(new SearchSourceBuilder()
                .size(1000)
                .query(finalQuery));
        //.fetchSource(false));
        //log.debug("createMatchingTerminologiesQuery Query request: " + sr.toString());
        return sr;
    }

    public Set<String> parseMatchingTerminologiesResponse(SearchResponse response) {
        Set<String> ret = new HashSet<>();
        for (SearchHit hit : response.getHits()) {
            try {
                JsonNode terminology = objectMapper.readTree(hit.getSourceAsString());
                //ret.add(hit.getId());
                ret.add(terminology.get("type").get("graph").get("id").textValue());
            } catch (Exception e) {
                log.error("Cannot parse matching terminologies response", e);
            }
        }
        return ret;
    }

    public TerminologySearchResponse parseResponse(SearchResponse response,
                                                   TerminologySearchRequest request,
                                                   Map<String, List<DeepSearchHitListDTO<?>>> deepSearchHitList) {
        List<TerminologyDTO> terminologies = new ArrayList<>();
        TerminologySearchResponse ret = new TerminologySearchResponse(0, pageFrom(request), terminologies, deepSearchHitList);
        try {
            SearchHits hits = response.getHits();
            ret.setTotalHitCount(hits.getTotalHits());
            Pattern highlightPattern = ElasticRequestUtils.createHighlightPattern(request.getQuery());
            for (SearchHit hit : hits) {
                JsonNode terminology = objectMapper.readTree(hit.getSourceAsString());
                // NOTE: terminology.get("id") would make more sense, but currently concepts contain only graph id => use it here also.
                String terminologyId = terminology.get("type").get("graph").get("id").textValue();
                String terminologyCode = ElasticRequestUtils.getTextValueOrNull(terminology, "code");
                String terminologyUri = ElasticRequestUtils.getTextValueOrNull(terminology, "uri");

                JsonNode properties = terminology.get("properties");
                JsonNode statusArray = properties.get("status");
                String terminologyStatus = statusArray != null ? (statusArray.has(0) ? statusArray.get(0).get("value").textValue() : "DRAFT") : "DRAFT";
                Map<String, String> labelMap = ElasticRequestUtils.labelFromLangValueArray(properties.get("prefLabel"));
                Map<String, String> descriptionMap = ElasticRequestUtils.labelFromLangValueArray(properties.get("description"));

                ElasticRequestUtils.highlightLabel(labelMap, highlightPattern);

                JsonNode references = terminology.get("references");
                JsonNode domainArray = references.get("inGroup");
                JsonNode contributorArray = references.get("contributor");
                List<InformationDomainDTO> domains = new ArrayList<>();
                List<OrganizationDTO> contributors = new ArrayList<>();
                if (domainArray != null) {
                    for (JsonNode domain : domainArray) {
                        String domainId = domain.get("id").textValue();
                        Map<String, String> domainLabel = ElasticRequestUtils.labelFromLangValueArray(domain.get("properties").get("prefLabel"));
                        domains.add(new InformationDomainDTO(domainId, domainLabel));
                    }
                }
                if (contributorArray != null) {
                    for (JsonNode contributor : contributorArray) {
                        String orgId = contributor.get("id").textValue();
                        Map<String, String> orgLabel = ElasticRequestUtils.labelFromLangValueArray(contributor.get("properties").get("prefLabel"));
                        contributors.add(new OrganizationDTO(orgId, orgLabel));
                    }
                }

                terminologies.add(new TerminologyDTO(terminologyId, terminologyCode, terminologyUri, terminologyStatus, labelMap, descriptionMap, domains, contributors));

            }
        } catch (Exception e) {
            log.error("Cannot parse terminology query response", e);
        }
        return ret;
    }

    private int pageSize(TerminologySearchRequest request) {
        Integer size = request.getPageSize();
        if (size != null && size >= 0) {
            return size.intValue();
        }
        return DEFAULT_PAGE_SIZE;
    }

    private int pageFrom(TerminologySearchRequest request) {
        Integer from = request.getPageFrom();
        if (from != null && from >= 0) {
            return from.intValue();
        }
        return DEFAULT_PAGE_FROM;
    }

    private QueryBuilder combineIncompleteQuery(QueryBuilder query,
                                                QueryBuilder incompleteQuery,
                                                boolean superUser) {
        if (superUser) {
            return query;
        }
        return QueryBuilders.boolQuery()
            .must(incompleteQuery)
            .must(query);
    }

    private QueryBuilder statusAndContributorQuery(Set<String> privilegedOrganizations) {
        // Content must either be in some other state than INCOMPLETE, or the user must match a contributor organization.
        QueryBuilder statusQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.matchQuery("properties.status.value", "INCOMPLETE"));
        QueryBuilder privilegeQuery;
        if (privilegedOrganizations != null && !privilegedOrganizations.isEmpty()) {
            privilegeQuery = QueryBuilders.boolQuery()
                .should(statusQuery)
                .should(QueryBuilders.termsQuery("references.contributor.id", privilegedOrganizations))
                .minimumShouldMatch(1);
        } else {
            privilegeQuery = statusQuery;
        }
        return privilegeQuery;
    }
}
