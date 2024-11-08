package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.opensearch.IndexBase;
import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.common.opensearch.SearchResponseDTO;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.*;
import fi.vm.yti.terminology.api.v2.opensearch.*;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static fi.vm.yti.common.opensearch.OpenSearchUtil.logPayload;

@Service
public class SearchIndexService {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexService.class);

    private final OpenSearchClientWrapper client;

    private final GroupManagementService groupManagementService;

    private final TerminologyService terminologyService;


    public SearchIndexService(OpenSearchClientWrapper client,
                              GroupManagementService groupManagementService,
                              TerminologyService terminologyService) {
        this.client = client;
        this.groupManagementService = groupManagementService;
        this.terminologyService = terminologyService;
    }

    public SearchResponseDTO<TerminologySearchResultDTO> searchTerminologies(
            TerminologySearchRequest request,
            YtiUser user) {
        // list of organizations that the user belongs to
        Set<UUID> privilegedOrganizations = user.isSuperuser() ?
                Collections.emptySet() :
                this.groupManagementService.getOrganizationsForUser(user);

        var matchingConcepts = getMatchingConcepts(request, user, privilegedOrganizations);

        var query = TerminologyQueryFactory.createTerminologyQuery(
                request,
                user.isSuperuser(),
                matchingConcepts,
                privilegedOrganizations);
        var terminologies = client.search(query, TerminologySearchResultDTO.class);

        if (matchingConcepts != null) {
            for (var searchResult : terminologies.getResponseObjects()) {
                // link each matching concept to the terminology, if the id matches
                matchingConcepts.getResponseObjects().stream()
                        .filter(o -> o.getNamespace().matches(searchResult.getUri()))
                        .forEach(searchResult::addMatchingConcept);
            }
        }

        return terminologies;
    }

    public SearchResponseDTO<ConceptSearchResultDTO> searchConcepts(
            ConceptSearchRequest request,
            YtiUser user) {
        Set<UUID> privilegedOrganizations = user.isSuperuser() ?
                Collections.emptySet() :
                this.groupManagementService.getOrganizationsForUser(user);
        Set<String> incompleteFromTerminologies = user.isSuperuser() ?
                Collections.emptySet() :
                terminologiesMatchingOrganizations(privilegedOrganizations);

        var query = ConceptQueryFactory.createConceptQuery(request, user.isSuperuser(), incompleteFromTerminologies);
        var concepts = client.search(query, ConceptSearchResultDTO.class);

        if (request.isExtendTerminologies()) {
            var terminologyIds = concepts.getResponseObjects().stream()
                    .map(IndexConcept::getNamespace)
                    .collect(Collectors.toSet());

            var terminologiesQuery = TerminologyQueryFactory.createFetchTerminologiesByNamespaceQuery(terminologyIds);
            var terminologies = client.search(terminologiesQuery, TerminologySearchResultDTO.class);

            concepts.getResponseObjects().forEach(r -> {
                var terminology = terminologies.getResponseObjects().stream()
                        .filter(t -> t.getUri().equals(r.getNamespace()))
                        .findFirst();
                terminology.ifPresent(t -> {
                    var dto = new SimpleTerminologyDTO();
                    dto.setPrefix(t.getPrefix());
                    dto.setLabel(t.getLabel());
                    r.setTerminology(dto);
                });
            });
        }
        return concepts;
    }

    private Set<String> terminologiesMatchingOrganizations(Set<java.util.UUID> privilegedOrganizations) {
        try {
            if (privilegedOrganizations.isEmpty()) {
                return Collections.emptySet();
            }
            var query = TerminologyQueryFactory.createMatchingTerminologiesQuery(
                    privilegedOrganizations);
            logPayload(query, IndexService.TERMINOLOGY_INDEX);
            var terminologies = client.search(query, TerminologySearchResultDTO.class);
            return new HashSet<>(terminologies
                    .getResponseObjects()
                    .stream()
                    .map(TerminologySearchResultDTO::getUri)
                    .toList());
        } catch (Exception e) {
            logger.error("Failed to resolve terminologies based on contributors", e);
            throw new RuntimeException(e);
        }
    }

    public CountSearchResponse getTerminologyCounts(TerminologySearchRequest request, YtiUser user) {
        request.setSearchConcepts(true);
        // add all statuses to the search because only particular statuses are included to the search by default
        if (request.getStatus() == null || request.getStatus().isEmpty()) {
            request.setStatus(Arrays.stream(Status.values()).collect(Collectors.toSet()));
        }
        if (request.getQuery() == null) {
            request.setQuery("");
        }

        Set<UUID> privilegedOrganizations = user.isSuperuser() ?
                Collections.emptySet() :
                this.groupManagementService.getOrganizationsForUser(user);

        var matchingConcepts = getMatchingConcepts(request, user, privilegedOrganizations);

        Set<String> additionalTerminologies = new HashSet<>();
        if (matchingConcepts != null) {
            additionalTerminologies = matchingConcepts.getResponseObjects().stream()
                    .map(IndexConcept::getNamespace)
                    .collect(Collectors.toSet());
        }
        var countRequest = CountQueryFactory.createTerminologyCountQuery(request, user,
                additionalTerminologies,
                privilegedOrganizations);

        var response = client.searchResponse(countRequest, IndexTerminology.class);

        var countResponse = new CountSearchResponse();
        var counts = new CountDTO(
                getBucketValues(response, "statuses"),
                getBucketValues(response, "languages"),
                getBucketValues(response, "types"),
                getBucketValues(response, "groups")
        );
        countResponse.setTotalHitCount(response.hits().total().value());
        countResponse.setCounts(counts);
        return countResponse;
    }

    public CountSearchResponse getConceptCounts(ConceptSearchRequest request, YtiUser user) {

        Set<UUID> privilegedOrganizations = user.isSuperuser() ?
                Collections.emptySet() :
                this.groupManagementService.getOrganizationsForUser(user);

        Set<String> incompleteFromTerminologies = user.isSuperuser() ?
                Collections.emptySet() :
                terminologiesMatchingOrganizations(privilegedOrganizations);

        var searchRequest = CountQueryFactory.createConceptCountQuery(request, user.isSuperuser(), incompleteFromTerminologies);

        var countSearchResponse = client.searchResponse(searchRequest, IndexConcept.class);

        // find concept collection count by type with sparql query, because they are not indexed.
        var collectionCount = terminologyService.getConceptCollectionCount(request.getNamespace());

        var countDTO = new CountDTO(
                getBucketValues(countSearchResponse, "statuses"),
                Map.of(
                        "Concept", countSearchResponse.hits().total().value(),
                        "Collection", collectionCount)
        );
        var response = new CountSearchResponse();
        response.setCounts(countDTO);
        response.setTotalHitCount(0);

        return response;
    }

    private SearchResponseDTO<ConceptSearchResultDTO> getMatchingConcepts(TerminologySearchRequest request, YtiUser user, Set<UUID> privilegedOrganizations) {
        if (request.isSearchConcepts() && !request.getQuery().isEmpty()) {
            // list of terminologies that the user has access to
            Set<String> incompleteFromTerminologies = user.isSuperuser() ?
                    Collections.emptySet() :
                    terminologiesMatchingOrganizations(privilegedOrganizations);

            var conceptRequest = new ConceptSearchRequest();
            conceptRequest.setQuery(request.getQuery());
            var query = ConceptQueryFactory.createConceptQuery(
                    conceptRequest,
                    user.isSuperuser(),
                    incompleteFromTerminologies);
            return client.search(query, ConceptSearchResultDTO.class);
        }
        return null;
    }

    private static Map<String, Long> getBucketValues(SearchResponse<? extends IndexBase> response, String aggregateName) {
        var aggregation = response.aggregations().get(aggregateName);

        if (aggregation == null) {
            return Collections.emptyMap();
        }
        return aggregation
                .sterms()
                .buckets()
                .array()
                .stream()
                .collect(Collectors.toMap(StringTermsBucket::key, StringTermsBucket::docCount));
    }
}
