package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.common.opensearch.SearchResponseDTO;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.ConceptSearchResultDTO;
import fi.vm.yti.terminology.api.v2.dto.TerminologySearchResultDTO;
import fi.vm.yti.terminology.api.v2.opensearch.ConceptQueryFactory;
import fi.vm.yti.terminology.api.v2.opensearch.ConceptSearchRequest;
import fi.vm.yti.terminology.api.v2.opensearch.TerminologyQueryFactory;
import fi.vm.yti.terminology.api.v2.opensearch.TerminologySearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

import static fi.vm.yti.common.opensearch.OpenSearchUtil.logPayload;

@Service
public class SearchIndexService {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchClientWrapper.class);

    private final OpenSearchClientWrapper client;

    private final GroupManagementService groupManagementService;

    private final TerminologyService terminologyService;

    private final ConceptService conceptService;

    private final ConceptCollectionService conceptCollectionService;

    public SearchIndexService(OpenSearchClientWrapper client,
                              GroupManagementService groupManagementService,
                              TerminologyService terminologyService,
                              ConceptService conceptService,
                              ConceptCollectionService conceptCollectionService) {
        this.client = client;
        this.groupManagementService = groupManagementService;
        this.terminologyService = terminologyService;
        this.conceptService = conceptService;
        this.conceptCollectionService = conceptCollectionService;
    }

    public SearchResponseDTO<TerminologySearchResultDTO> searchTerminologies(
            TerminologySearchRequest request,
            YtiUser user) {
        // list of organizations that the user belongs to
        Set<java.util.UUID> privilegedOrganizations = user.isSuperuser() ?
                Collections.emptySet() :
                this.groupManagementService.getOrganizationsForUser(user);

        SearchResponseDTO<ConceptSearchResultDTO> matchingConcepts = null;
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
            matchingConcepts = client.search(query, ConceptSearchResultDTO.class);
        }

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
        Set<java.util.UUID> privilegedOrganizations = user.isSuperuser() ?
                Collections.emptySet() :
                this.groupManagementService.getOrganizationsForUser(user);
        Set<String> incompleteFromTerminologies = user.isSuperuser() ?
                Collections.emptySet() :
                terminologiesMatchingOrganizations(privilegedOrganizations);

        var query = ConceptQueryFactory.createConceptQuery(request, user.isSuperuser(), incompleteFromTerminologies);
        var concepts = client.search(query, ConceptSearchResultDTO.class);

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
}
