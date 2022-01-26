package fi.vm.yti.terminology.api.frontend;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import fi.vm.yti.terminology.api.frontend.elasticqueries.CountQueryFactory;
import fi.vm.yti.terminology.api.frontend.searchdto.*;
import fi.vm.yti.terminology.api.util.RestHighLevelClientWrapper;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.frontend.elasticqueries.ConceptQueryFactory;
import fi.vm.yti.terminology.api.frontend.elasticqueries.DeepConceptQueryFactory;
import fi.vm.yti.terminology.api.frontend.elasticqueries.TerminologyQueryFactory;
import fi.vm.yti.terminology.api.util.Parameters;
import static fi.vm.yti.terminology.api.util.ElasticRequestUtils.responseContentAsString;

@Service
public class FrontendElasticSearchService {

    private static final Logger logger = LoggerFactory.getLogger(FrontendElasticSearchService.class);

    private RestHighLevelClientWrapper esRestClient;

    private final String indexName;
    private final String indexMappingType;

    private final ObjectMapper objectMapper;
    private final AuthenticatedUserProvider userProvider;
    private final TerminologyQueryFactory terminologyQueryFactory;
    private final DeepConceptQueryFactory deepConceptQueryFactory;
    private final CountQueryFactory countQueryFactory;
    private final ConceptQueryFactory conceptQueryFactory;

    @Autowired
    public FrontendElasticSearchService(@Value("${search.index.name}") String indexName,
                                        @Value("${search.index.mapping.type}") String indexMappingType,
                                        @Value("${namespace.root}") String namespaceRoot,
                                        RestHighLevelClientWrapper esRestClient,
                                        ObjectMapper objectMapper,
                                        AuthenticatedUserProvider userProvider) {
        this.indexName = indexName;
        this.indexMappingType = indexMappingType;
        this.esRestClient = esRestClient;
        this.objectMapper = objectMapper;
        this.userProvider = userProvider;
        this.terminologyQueryFactory = new TerminologyQueryFactory(objectMapper);
        this.deepConceptQueryFactory = new DeepConceptQueryFactory(objectMapper);
        this.conceptQueryFactory = new ConceptQueryFactory(objectMapper, namespaceRoot);
        this.countQueryFactory = new CountQueryFactory(objectMapper);
    }

    ConceptSearchResponse searchConcept(ConceptSearchRequest request) {
        request.setQuery(request.getQuery() != null ? request.getQuery().trim() : "");
        try {
            final boolean superUser = superUser();
            SearchRequest query = conceptQueryFactory.createQuery(request, superUser, limit ->
                superUser ? Collections.emptySet() : terminologiesMatchingOrganizations(readOrganizations(), limit)
            );
            SearchResponse response = esRestClient.search(query, RequestOptions.DEFAULT);
            return conceptQueryFactory.parseResponse(response, request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    TerminologySearchResponse searchTerminology(TerminologySearchRequest request) {
        request.setQuery(request.getQuery() != null ? request.getQuery().trim() : "");

        boolean superUser = superUser();
        Set<String> privilegedOrganizations = superUser ? Collections.emptySet() : readOrganizations();

        Map<String, List<DeepSearchHitListDTO<?>>> deepSearchHits = null;
        if (request.isSearchConcepts() && !request.getQuery().isEmpty()) {
            try {
                Set<String> incompleteFromTerminologies = superUser ?
                        Collections.emptySet() :
                        terminologiesMatchingOrganizations(privilegedOrganizations, null);
                SearchRequest query = deepConceptQueryFactory.createQuery(
                        request.getQuery(),
                        request.getStatuses(),
                        request.getPrefLang(),
                        superUser,
                        incompleteFromTerminologies);
                // logger.debug("deepConceptQuery: " + query.toString());
                SearchResponse response = esRestClient.search(query, RequestOptions.DEFAULT);
                deepSearchHits = deepConceptQueryFactory.parseResponse(response, request);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            SearchRequest finalQuery;
            if (deepSearchHits != null && !deepSearchHits.isEmpty()) {
                Set<String> additionalTerminologyIds = deepSearchHits.keySet();
                logger.debug("Deep concept search resulted in " + additionalTerminologyIds.size() + " terminology matches");
                finalQuery = terminologyQueryFactory.createQuery(request, additionalTerminologyIds, superUser, privilegedOrganizations);
            } else {
                finalQuery = terminologyQueryFactory.createQuery(request, superUser, privilegedOrganizations);
            }
            SearchResponse response = esRestClient.search(finalQuery, RequestOptions.DEFAULT);
            return terminologyQueryFactory.parseResponse(response, request, deepSearchHits);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    CountSearchResponse getCounts() {
        SearchRequest query = countQueryFactory.createQuery();
        try {
            SearchResponse response = esRestClient.search(query, RequestOptions.DEFAULT);
            logger.debug(response.toString());
            return countQueryFactory.parseResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    CountSearchResponse getConceptCounts(UUID vocabularyId)  {
        SearchRequest request = countQueryFactory.createConceptCountQuery(vocabularyId);
        try {
            SearchResponse response = esRestClient.search(request, RequestOptions.DEFAULT);
            return countQueryFactory.parseResponse(response);
        } catch (IOException e) {
            logger.error("Error fetching concept counts", e);
            throw new RuntimeException(e);
        }
    }

    private boolean superUser() {
        return userProvider.getUser().isSuperuser();
    }

    private Set<String> readOrganizations() {
        // Any role is OK for reading (viewing data).
        return userProvider.getUser().getRolesInOrganizations().entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .map(entry -> entry.getKey().toString())
            .collect(Collectors.toSet());
    }

    private Set<String> terminologiesMatchingOrganizations(Collection<String> privilegedOrganizations,
                                                           Collection<String> limitToThese) {
        try {
            if (privilegedOrganizations.isEmpty()) {
                return Collections.emptySet();
            }
            SearchRequest sr = terminologyQueryFactory.createMatchingTerminologiesQuery(privilegedOrganizations, limitToThese);
            logger.debug("terminologiesMatchingOrganizations query: " + sr.toString());
            SearchResponse response = esRestClient.search(sr, RequestOptions.DEFAULT);
            return terminologyQueryFactory.parseMatchingTerminologiesResponse(response);
        } catch (Exception e) {
            logger.error("Failed to resolve terminologies based on contributors", e);
        }
        return Collections.emptySet();
    }
}
