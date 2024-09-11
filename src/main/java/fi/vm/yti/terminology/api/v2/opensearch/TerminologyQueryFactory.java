package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.common.opensearch.SearchResponseDTO;
import fi.vm.yti.common.opensearch.QueryFactoryUtils;
import fi.vm.yti.terminology.api.v2.dto.ConceptSearchResultDTO;
import fi.vm.yti.terminology.api.v2.service.IndexService;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static fi.vm.yti.common.opensearch.OpenSearchUtil.logPayload;

public class TerminologyQueryFactory {

    private static final Logger logger = LoggerFactory.getLogger(TerminologyQueryFactory.class);

    private TerminologyQueryFactory() {
    }

    public static SearchRequest createTerminologyQuery(
            TerminologySearchRequest request,
            boolean isSuperUser,
            Set<UUID> privilegedOrganizations) {
        return TerminologyQueryFactory.createTerminologyQuery(
                request,
                isSuperUser,
                null,
                privilegedOrganizations);
    }

    public static SearchRequest createTerminologyQuery(
            TerminologySearchRequest request,
            boolean isSuperUser,
            SearchResponseDTO<ConceptSearchResultDTO> deepSearchHits,
            Set<UUID> privilegedOrganizations) {

        Set<String> additionalTerminologyUris = deepSearchHits == null ?
                Set.of() :
                deepSearchHits
                        .getResponseObjects()
                        .stream()
                        .map(ConceptSearchResultDTO::getUri)
                        // we got a list of concepts, and their terminology is part of the uri
                        .map(uri -> uri.substring(0, uri
                                .replaceAll("/$", "")
                                .lastIndexOf("/")) + "/")
                        .collect(Collectors.toSet());

        logger.debug("Deep concept search resulted in " + additionalTerminologyUris.size() + " terminology matches");

        var terminologyQuery = getTerminologyBaseQuery(request, isSuperUser, additionalTerminologyUris, privilegedOrganizations);

        Highlight.Builder highlight = new Highlight.Builder();
        highlight.fields("label.*", f -> f);
        var sr = new SearchRequest.Builder()
                .index(IndexService.TERMINOLOGY_INDEX)
                .size(QueryFactoryUtils.pageSize(request.getPageSize()))
                .from(QueryFactoryUtils.pageFrom(request.getPageFrom()))
                .sort(QueryFactoryUtils.getLangSortOptions(request.getSortLang()))
                .highlight(highlight.build())
                .query(terminologyQuery)
                .build();

        logPayload(sr, IndexService.TERMINOLOGY_INDEX);

        return sr;
    }

    private static Query getTerminologyBaseQuery(
            TerminologySearchRequest request,
            boolean isSuperUser,
            Set<String> additionalTerminologyUris,
            Set<UUID> privilegedOrganizations) {

        // separate sections within should for the user query and
        // terminologies from deep search results
        var shouldQueries = new ArrayList<Query>();
        var mustQueries = new ArrayList<Query>();

        var excludeIncomplete = new ArrayList<Query>();

        //
        // Filter out DRAFT unless superuser or has access to the terminology
        //
        if (privilegedOrganizations != null && !privilegedOrganizations.isEmpty()) {
            var incompleteFromQuery = QueryFactoryUtils.termsQuery(
                    "organizations",
                    privilegedOrganizations
                            .stream()
                            .map(UUID::toString)
                            .toList());
            excludeIncomplete.add(incompleteFromQuery);
        }

        if (!isSuperUser) {
            excludeIncomplete.add(BoolQuery.of(bq -> bq
                            .mustNot(TermQuery.of(tq -> tq
                                            .field("status")
                                            .value(FieldValue.of("INCOMPLETE")))
                                    .toQuery()))
                    .toQuery());
        }

        if (excludeIncomplete.size() == 1) {
            mustQueries.add(excludeIncomplete.get(0));
        } else if (excludeIncomplete.size() > 1) {
            mustQueries.add(BoolQuery
                    .of(bq -> bq
                            .should(excludeIncomplete)
                            .minimumShouldMatch("1"))
                    .toQuery());
        }

        //
        // Filter by query string
        //
        var queryString = request.getQuery();
        if (queryString != null && !queryString.isBlank()) {
            mustQueries.add(QueryBuilders.bool()
                    .should(QueryFactoryUtils.labelQuery(queryString))
                    .minimumShouldMatch("1")
                    .build()
                    .toQuery());
        }

        //
        // Filter by status
        //
        if (request.getStatus() != null) {
            mustQueries.add(TermsQuery.of(q -> q
                            .field("status")
                            .terms(t -> t.value(request
                                    .getStatus()
                                    .stream()
                                    .map(Enum::name)
                                    .map(FieldValue::of)
                                    .toList())))
                    .toQuery());
        }

        //
        // Filter by organizations
        //
        if (request.getOrganizations() != null && !request.getOrganizations().isEmpty()) {
            mustQueries.add(TermsQuery.of(q -> q
                            .field("organizations")
                            .terms(t -> t.value(request
                                    .getOrganizations()
                                    .stream()
                                    .map(UUID::toString)
                                    .map(FieldValue::of)
                                    .toList())))
                    .toQuery());
        }

        //
        // Filter by groups
        //
        if (request.getGroups() != null && !request.getGroups().isEmpty()) {
            mustQueries.add(TermsQuery.of(q -> q
                            .field("groups")
                            .terms(t -> t.value(request.getGroups()
                                    .stream()
                                    .map(FieldValue::of)
                                    .toList())))
                    .toQuery());
        }

        //
        // Filter by languages
        //
        if (request.getLanguages() != null && !request.getLanguages().isEmpty()) {
            mustQueries.add(TermsQuery.of(q -> q
                            .field("languages")
                            .terms(t -> t.value(request.getLanguages()
                                    .stream()
                                    .map(FieldValue::of)
                                    .toList())))
                    .toQuery());
        }

        //
        // Results from deep concept search
        //
        if (additionalTerminologyUris != null && !additionalTerminologyUris.isEmpty()) {
            shouldQueries.add(QueryBuilders.bool()
                    .should(TermsQuery.of(q -> q
                                    .field("uri")
                                    .terms(t -> t.value(additionalTerminologyUris
                                            .stream()
                                            .map(FieldValue::of)
                                            .toList())))
                            .toQuery())
                    .minimumShouldMatch("1")
                    .build()
                    .toQuery());
        }

        //
        // Construct final query
        //
        Query mustQuery;
        if (mustQueries.size() == 1) {
            mustQuery = mustQueries.get(0);
        } else if (mustQueries.size() > 1) {
            mustQuery = QueryBuilders.bool().must(mustQueries).build().toQuery();
        } else {
            mustQuery = QueryBuilders.matchAll().build().toQuery();
        }

        Query shouldQuery = null;
        if (!shouldQueries.isEmpty()) {
            shouldQueries.add(mustQuery);
            shouldQuery = QueryBuilders
                    .bool()
                    .should(shouldQueries)
                    .minimumShouldMatch("1")
                    .build()
                    .toQuery();
        }

        return shouldQuery != null ? shouldQuery : mustQuery;
    }

    public static SearchRequest createMatchingTerminologiesQuery(
            final Collection<UUID> privilegedOrganizations) {
        var q1 = TermsQuery.of(q -> q
                        .field("organizations")
                        .terms(t -> t.value(privilegedOrganizations
                                .stream()
                                .map(UUID::toString)
                                .map(FieldValue::of)
                                .toList())))
                .toQuery();
        return new SearchRequest.Builder()
                .index(IndexService.TERMINOLOGY_INDEX)
                .size(10000)
                .query(q1)
                .build();
    }

    public static SearchRequest createFetchTerminologiesByNamespaceQuery(Set<String> namespaces) {
        var query = TermsQuery.of(q -> q
                .field("uri")
                .terms(t -> t.value(namespaces
                        .stream()
                        .map(FieldValue::of)
                        .toList())
                )).toQuery();
        return new SearchRequest.Builder()
                .index(IndexService.TERMINOLOGY_INDEX)
                .size(10000)
                .query(query)
                .build();
    }
}
