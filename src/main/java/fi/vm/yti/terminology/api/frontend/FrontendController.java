package fi.vm.yti.terminology.api.frontend;

import java.util.*;

import fi.vm.yti.terminology.api.exception.NamespaceInUseException;
import fi.vm.yti.terminology.api.exception.VocabularyNotFoundException;
import fi.vm.yti.terminology.api.frontend.searchdto.*;
import fi.vm.yti.terminology.api.validation.ValidVocabularyNode;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.model.termed.GenericDeleteAndSave;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.GenericNodeInlined;
import fi.vm.yti.terminology.api.model.termed.Graph;
import fi.vm.yti.terminology.api.model.termed.Identifier;
import fi.vm.yti.terminology.api.model.termed.MetaNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.validation.constraints.Size;

import static fi.vm.yti.terminology.api.model.termed.NodeType.Group;
import static fi.vm.yti.terminology.api.model.termed.NodeType.Organization;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/api/v1/frontend")
@Tag(name = "Frontend")
@Validated
public class FrontendController {

    private final FrontendTermedService termedService;
    private final FrontendElasticSearchService elasticSearchService;
    private final FrontendGroupManagementService groupManagementService;
    private final AuthenticatedUserProvider userProvider;
    private final ObjectMapper objectMapper;
    private final String namespaceRoot;
    private final String groupManagementUrl;
    private final boolean fakeLoginAllowed;

    private static final Logger logger = LoggerFactory.getLogger(FrontendController.class);

    public FrontendController(FrontendTermedService termedService,
                              FrontendElasticSearchService elasticSearchService,
                              FrontendGroupManagementService groupManagementService,
                              ObjectMapper objectMapper,
                              AuthenticatedUserProvider userProvider,
                              @Value("${namespace.root}") String namespaceRoot,
                              @Value("${groupmanagement.public.url}") String groupManagementUrl,
                              @Value("${fake.login.allowed:false}") boolean fakeLoginAllowed) {
        this.termedService = termedService;
        this.elasticSearchService = elasticSearchService;
        this.groupManagementService = groupManagementService;
        this.userProvider = userProvider;
        this.objectMapper = objectMapper;
        this.namespaceRoot = namespaceRoot;
        this.groupManagementUrl = groupManagementUrl;
        this.fakeLoginAllowed = fakeLoginAllowed;
    }

    @Operation(summary = "Get YTI Group Management Service URL", description = "Get YTI Group Management Service URL (root URL) from system configuration")
    @ApiResponse(responseCode = "200", description = "YTI Group Management Service URL (root URL) as a string")
    @GetMapping(path = "/groupManagementUrl", produces = APPLICATION_JSON_VALUE)
    String getGroupManagementUrl() {
        logger.info("GET /groupManagementUrl requested");
        return groupManagementUrl;
    }

    @Operation(summary = "Get list of test users", description = "If impersonating test users is enabled then returns list of test users")
    @ApiResponse(responseCode = "200", description = "List of test users for impersonating, or empty list if not an development environment")
    @GetMapping(path = "/fakeableUsers", produces = APPLICATION_JSON_VALUE)
    List<GroupManagementUser> getFakeableUsers() {
        logger.info("GET /fakeableUsers requested");

        if (fakeLoginAllowed) {
            return groupManagementService.getUsers();
        } else {
            return Collections.emptyList();
        }
    }

    @Operation(summary = "Check whether a terminology namespace prefix is in use")
    @ApiResponse(responseCode = "200", description = "True if prefix is reserved, false if it is free for use with new terminology")
    @GetMapping(path = "/namespaceInUse", produces = APPLICATION_JSON_VALUE)
    boolean isNamespaceInUse(@Parameter(description = "Freely selectable namespace part of a terminology, here called \"prefix\"") @RequestParam String prefix) {
        logger.info("GET /namespaceInUse requested with prefix: " + prefix);
        return termedService.isNamespaceInUse(prefix);
    }

    @Operation(summary = "Get the static portion of terminology namespaces")
    @ApiResponse(responseCode = "200", description = "Terminology namespace root, i.e., the prefix for all terminology namespaces")
    @GetMapping(path = "/namespaceRoot", produces = APPLICATION_JSON_VALUE)
    String getNamespaceRoot() {
        logger.info("GET /namespaceRoot requested");
        return namespaceRoot;
    }

    @Operation(summary = "Get the currently authenticated use, i.e., the caller")
    @ApiResponse(responseCode = "200", description = "User object for the caller")
    @RequestMapping(path = "/authenticated-user", method = GET, produces = APPLICATION_JSON_VALUE)
    YtiUser getUser() {
        logger.info("GET /authenticated-user requested");
        return userProvider.getUser();
    }

    @Operation(summary = "Get list of authorization requests for the current user", description = "Get the currently authenticated user's pending requests for roles for organizations")
    @ApiResponse(responseCode = "200", description = "The currently authenticated user's pending requests for roles for organizations")
    @ApiResponse(responseCode = "401", description = "If the caller is not not authenticated user")
    @RequestMapping(path = "/requests", method = GET, produces = APPLICATION_JSON_VALUE)
    List<GroupManagementUserRequest> getUserRequests() {
        logger.info("GET /requests requested");
        return groupManagementService.getUserRequests();
    }

    @Operation(summary = "Request authorization for a organization", description = "Request to be added to an organization in TERMINOLOGY_EDITOR role")
    @ApiResponse(responseCode = "200", description = "Request submitted successfully")
    @ApiResponse(responseCode = "401", description = "If the caller is not not authenticated user")
    @PostMapping(path = "/request", produces = APPLICATION_JSON_VALUE)
    void sendRequest(
            @Parameter(description = "UUID for the organization") @RequestParam UUID organizationId,
            @Parameter(description = "Comma separated list of roles for organisation") @RequestParam(required = false) String[] roles) {
        logger.info("POST /request requested with organizationID: " + organizationId.toString());
        groupManagementService.sendRequest(organizationId, roles);
    }

    @Operation(summary = "Get terminology basic info as JSON")
    @ApiResponse(responseCode = "200", description = "The requested terminology node data")
    @GetMapping(path = "/vocabulary", produces = APPLICATION_JSON_VALUE)
    GenericNodeInlined getVocabulary(@Parameter(description = "ID for the requested terminology") @RequestParam UUID graphId) {
        logger.info("GET /vocabulary requested with graphId: " + graphId.toString());
        return termedService.getVocabulary(graphId);
    }

    @Operation(summary = "Get basic info for all terminologies", description = "Get basic info for termonologies in Termed JSON format. The list may be filtered for INCOMPLETE terminologies.")
    @Parameter(
        name = "incomplete",
        description = "Super users get always all terminologies. Other users see INCOMPLETE terminologies according to their organization " +
            "roles. If this parameter is set to false no INCOMPLETE terminologies are returned for normal users.")
    @ApiResponse(responseCode = "200", description = "The basic info for terminologies in unprocessed Termed JSON format")
    @GetMapping(path = "/vocabularies", produces = APPLICATION_JSON_VALUE)
    JsonNode getVocabularyList(@RequestParam(required = false, defaultValue = "true") boolean incomplete) {
        logger.info("GET /vocabularies requested incomplete=" + incomplete);
        return termedService.getVocabularyList(incomplete);
    }

    @Operation(summary = "Create a new terminology")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new terminology node")
    @ApiResponse(responseCode = "200", description = "The ID for the newly created terminology")
    @PostMapping(path = "/vocabulary", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    UUID createVocabulary(
            @Parameter(description = "The meta model graph for the new terminology")
            @RequestParam UUID templateGraphId,

            @Size(min = 3, message = "Prefix must be minimum of 3 characters")
            @Parameter(description = "The prefix, i.e., freely selectable part of terminology namespace")
            @RequestParam String prefix,

            @Parameter(description = "If given, tries to use the ID for the terminology")
            @RequestParam(required = false) @Nullable UUID graphId,

            @Parameter(description = "Whether to do synchronous creation, i.e., wait for the result. This is recommended.")
            @RequestParam(required = false, defaultValue = "true") boolean sync,

            @RequestBody GenericNode vocabularyNode) {

        try {
            logger.info("POST /vocabulary requested with params: templateGraphId: " +
                templateGraphId.toString() + ", prefix: " + prefix + ", vocabularyNode.id: " + vocabularyNode.getId().toString());

            UUID predefinedOrGeneratedGraphId = graphId != null ? graphId : UUID.randomUUID();

            termedService.createVocabulary(templateGraphId, prefix, vocabularyNode, predefinedOrGeneratedGraphId, sync);
            logger.debug("Vocabulary with prefix \"" + prefix + "\" created");

            return predefinedOrGeneratedGraphId;
        } catch (RuntimeException | Error e) {
            logger.error("createVocabulary failed", e);
            throw e;
        } finally {
            logger.debug("Vocabulary creation finished");
        }
    }

    @Operation(summary = "Create a new terminology")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new terminology node")
    @ApiResponse(responseCode = "200", description = "The ID for the newly created terminology")
    @PostMapping(path = "/validatedVocabulary", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    UUID createValidatedVocabulary(
            @Parameter(description = "The meta model graph for the new terminology")
            @RequestParam UUID templateGraphId,

            @Size(min = 3, message = "Prefix must be minimum of 3 characters")
            @Parameter(description = "The prefix, i.e., freely selectable part of terminology namespace")
            @RequestParam String prefix,

            @Parameter(description = "If given, tries to use the ID for the terminology")
            @RequestParam(required = false) @Nullable UUID graphId,

            @Parameter(description = "Whether to do synchronous creation, i.e., wait for the result. This is recommended.")
            @RequestParam(required = false, defaultValue = "true") boolean sync,

            @ValidVocabularyNode
            @RequestBody GenericNode vocabularyNode) {

        try {
            logger.info("POST /validatedVocabulary requested with params: templateGraphId: " +
                    templateGraphId.toString() + ", prefix: " + prefix + ", vocabularyNode.id: " + vocabularyNode.getId().toString());

            UUID predefinedOrGeneratedGraphId = graphId != null ? graphId : UUID.randomUUID();

            termedService.createVocabulary(templateGraphId, prefix, vocabularyNode, predefinedOrGeneratedGraphId, sync);
            logger.debug("Vocabulary with prefix \"" + prefix + "\" created");

            return predefinedOrGeneratedGraphId;
        } catch (RuntimeException | Error e) {
            logger.error("createVocabulary failed", e);
            throw e;
        } finally {
            logger.debug("Vocabulary creation finished");
        }
    }

    @Operation(summary = "Validate a terminology node")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the terminology node to validate")
    @ApiResponse(responseCode = "200", description = "OK on success")
    @PostMapping(path = "/vocabulary/validate", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    String validateVocabulary(
            @Parameter(description = "The meta model graph for the new terminology")
            @RequestParam UUID templateGraphId,

            @Size(min = 3, message = "Prefix must be minimum of 3 characters")
            @Parameter(description = "The prefix, i.e., freely selectable part of terminology namespace")
            @RequestParam String prefix,

            @Parameter(description = "If given, tries to use the ID for the terminology")
            @RequestParam(required = false) @Nullable UUID graphId,

            @ValidVocabularyNode
            @RequestBody GenericNode vocabularyNode) {

        logger.info("POST /vocabulary/validate requested with params: templateGraphId: " +
                templateGraphId.toString() + ", prefix: " +
                prefix + ", vocabularyNode.id: " + vocabularyNode.getId().toString());
        // if we got this far, the validation was successful
        return "OK";
    }

    @Operation(summary = "Delete a terminology")
    @ApiResponse(responseCode = "200", description = "Terminology deleted")
    @DeleteMapping(path = "/vocabulary", produces = APPLICATION_JSON_VALUE)
    void deleteVocabulary(@Parameter(description = "Id for the terminology to be deleted") @RequestParam UUID graphId) {
        logger.info("DELETE /vocabulary requested with graphId: " + graphId.toString());
        termedService.deleteVocabulary(graphId);
    }

    @Operation(summary = "Get concept basic info as JSON")
    @ApiResponse(responseCode = "200", description = "The requested concept node data")
    @GetMapping(path = "/concept", produces = APPLICATION_JSON_VALUE)
    @Nullable GenericNodeInlined getConcept(@Parameter(description = "Terminology ID") @RequestParam UUID graphId,
                                            @Parameter(description = "Concept ID") @RequestParam UUID conceptId) {
        logger.info("GET /concept requested with params: graphId: " + graphId.toString() + ", conceptId: " + conceptId.toString());
        return termedService.getConcept(graphId, conceptId);
    }

    @Operation(summary = "Get a concept collection as JSON")
    @ApiResponse(responseCode = "200", description = "The requested concept collection as JSON")
    @GetMapping(path = "/collection", produces = APPLICATION_JSON_VALUE)
    GenericNodeInlined getCollection(@Parameter(description = "Terminology ID") @RequestParam UUID graphId,
                                     @Parameter(description = "Concept collection ID") @RequestParam UUID collectionId) {
        logger.info("GET /collection requested with params: graphId: " + graphId.toString() + ", collectionId: " + collectionId.toString());
        return termedService.getCollection(graphId, collectionId);
    }

    @Operation(summary = "Get all concept collections for a terminology")
    @ApiResponse(responseCode = "200", description = "Concept collections for a terminology as JSON")
    @GetMapping(path = "/collections", produces = APPLICATION_JSON_VALUE)
    JsonNode getCollectionList(@Parameter(description = "Terminology ID") @RequestParam UUID graphId) {
        logger.info("GET /collections requested with graphId: " + graphId.toString());
        return termedService.getCollectionList(graphId);
    }

    @Operation(summary = "Get organization list", description = "Get organizations available at YTI Group Management Service")
    @ApiResponse(responseCode = "200", description = "The basic info for organizations in unprocessed Termed JSON format")
    @GetMapping(path = "/organizations", produces = APPLICATION_JSON_VALUE)
    JsonNode getOrganizationList() {
        logger.info("GET /organizations requested");
        return termedService.getNodeListWithoutReferencesOrReferrers(Organization);
    }

    @Operation(summary = "Get information domain list")
    @ApiResponse(responseCode = "200", description = "Information domain list in unprocessed Termed JSON format")
    @GetMapping(path = "/groups", produces = APPLICATION_JSON_VALUE)
    JsonNode getGroupList() {
        logger.info("GET /groups requested");
        return termedService.getNodeListWithoutReferencesOrReferrers(Group);
    }

    @Operation(summary = "Get organization list for v2", description = "Get organizations available at YTI Group Management Service")
    @ApiResponse(responseCode = "200", description = "The basic info for organizations in unprocessed Termed JSON format")
    @GetMapping(path = "/v2/organizations", produces = APPLICATION_JSON_VALUE)
    JsonNode getOrganizationListV2(@RequestParam(required = false, defaultValue = "fi") String language) {
        logger.info("GET /organizations requested");
        return termedService.getNodeListWithoutReferencesOrReferrersV2(Organization, language);
    }

    @Operation(summary = "Get information domain list for v2")
    @ApiResponse(responseCode = "200", description = "Information domain list in unprocessed Termed JSON format")
    @GetMapping(path = "/v2/groups", produces = APPLICATION_JSON_VALUE)
    JsonNode getGroupListV2(@RequestParam(required = false, defaultValue = "fi") String language) {
        logger.info("GET /groups requested");
        return termedService.getNodeListWithoutReferencesOrReferrersV2(Group, language);
    }

    @Operation(summary = "Make a bulk modification request", description = "Update and/or delete several nodes")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON for the bulk request containing nodes to be updated or deleted")
    @ApiResponse(responseCode = "200", description = "The operation was successful (valid if synchronous)")
    @PostMapping(path = "/modify", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    void updateAndDeleteInternalNodes(@Parameter(description = "Whether to do synchronous modification, i.e., wait for the result. This is recommended.")
                                      @RequestParam(required = false, defaultValue = "true") boolean sync,
                                      @RequestBody GenericDeleteAndSave deleteAndSave) {
        logger.info("POST /modify requested with deleteAndSave: delete ids: ");
        for (int i = 0; i < deleteAndSave.getDelete().size(); i++) {
            logger.info(deleteAndSave.getDelete().get(i).getId().toString());
        }
        logger.info("and save ids: ");
        for (int i = 0; i < deleteAndSave.getSave().size(); i++) {
            logger.info(deleteAndSave.getSave().get(i).getId().toString());
        }

        termedService.bulkChange(deleteAndSave, sync);
    }

    @Operation(summary = "Delete several nodes", description = "May be used, e.g., to delete a concept and its terms in one request")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON list of all the node IDs to delete")
    @ApiResponse(responseCode = "200", description = "Operation was successful (valid if synchronous)")
    @DeleteMapping(path = "/remove", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    void removeNodes(@Parameter(description = "Whether to do synchronous modification, i.e., wait for the result. This is recommended.")
                     @RequestParam boolean sync,
                     @Parameter(description = "Whether to disconnect removed nodes from other ones") @RequestParam boolean disconnect,
                     @RequestBody List<Identifier> identifiers) {
        logger.info("DELETE /remove requested with params: sync: " + sync + ", disconnect: " + disconnect + ", identifier ids: ");
        for (final Identifier ident : identifiers) {
            logger.info(ident.getId().toString());
        }
        termedService.removeNodes(sync, disconnect, identifiers);
    }

    @Operation(
            summary = "Update statuses",
            description = "Update statuses of several terms or concepts at once")
    @ApiResponse(
            responseCode = "200",
            description = "The operation was successful")
    @PostMapping(
            path = "/modifyStatuses",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    void updateStatuses(
            @Parameter(description = "Graph to update") @RequestParam UUID graphId,
            @Parameter(description = "Modify nodes matching this status") @RequestParam String oldStatus,
            @Parameter(description = "New status to assign to all matched nodes") @RequestParam String newStatus,
            @Parameter(description = "Modify nodes matching these types (Concept, Term)") @RequestParam Set<String> types) {
        logger.debug(
                "POST /modifyStatuses requested with graphId: {}, oldStatus: {}, newState: {}, types: {}",
                graphId.toString(),
                oldStatus,
                newStatus,
                String.join(",", types));
        termedService.modifyStatuses(graphId, types, oldStatus, newStatus);
    }

    @Operation(summary = "Get meta model")
    @ApiResponse(responseCode = "200", description = "Meta model nodes as JSON")
    @GetMapping(path = "/types", produces = APPLICATION_JSON_VALUE)
    List<MetaNode> getTypes(@Parameter(description = "If given then return meta model for a specific graph") @RequestParam(required = false) UUID graphId) {
        if (graphId != null) {
            logger.info("GET /types requested with graphId: " + graphId.toString());
        } else {
            logger.info("GET /types requested without graphId");
        }
        return termedService.getTypes(graphId);
    }

    @Operation(summary = "Get all graphs from Termed")
    @ApiResponse(responseCode = "200", description = "Basic info for all graphs, including terminology graph, meta model graph, information domain graph and organization graph")
    @GetMapping(path = "/graphs", produces = APPLICATION_JSON_VALUE)
    List<Graph> getGraphs() {
        logger.info("GET /graphs requested");
        return termedService.getGraphs();
    }

    @Operation(summary = "Get basic info for a graph")
    @ApiResponse(responseCode = "200", description = "Basic info for a graph")
    @GetMapping(path = "/graphs/{id}", produces = APPLICATION_JSON_VALUE)
    Graph getGraph(@Parameter(description = "Id for the graph") @PathVariable("id") UUID graphId) {
        logger.info("GET /graphs/{id} requested with graphId: " + graphId.toString());
        return termedService.getGraph(graphId);
    }

    @Operation(summary = "Search for concepts", description = "Make a concept search query based on query object")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Concept search query object as JSON")
    @ApiResponse(responseCode = "200", description = "Concept search response container object as JSON")
    @PostMapping(path = "/searchConcept", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    ConceptSearchResponse searchConceptNg(@RequestBody ConceptSearchRequest request) {
        logger.info("POST /searchConcept requested with query: " + request.toString());
        return elasticSearchService.searchConcept(request);
    }

    @Operation(summary = "Search for terminologies", description = "Make a terminology search query based on query object")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Terminology search query object as JSON")
    @ApiResponse(responseCode = "200", description = "Terminology search response container object as JSON")
    @RequestMapping(value = "/searchTerminology", method = POST, produces = APPLICATION_JSON_VALUE)
    TerminologySearchResponse searchTerminology(@RequestBody TerminologySearchRequest request) {
        logger.info("POST /searchTerminology requested with query: " + request.toString());
        return elasticSearchService.searchTerminology(request);
    }

    @Operation(summary = "Get counts", description = "List counts of concepts and vocabularies grouped by different search results")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List counts of concepts and vocabularies grouped by different search results")
    @ApiResponse(responseCode = "200", description = "Counts response container object as JSON")
    @RequestMapping(value = "/counts", method = GET, produces = APPLICATION_JSON_VALUE)
    CountSearchResponse getCounts(@RequestParam(required = false) boolean vocabularies) {
        logger.info("GET /counts requested with vocabulary: " + vocabularies);
        if (vocabularies) {
            return elasticSearchService.getVocabularyCounts();
        }
        return elasticSearchService.getCounts();
    }

    @Operation(summary = "Get concept and collection counts", description = "List counts of concepts and collections")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List counts of concepts and collections in particular vocabulary")
    @ApiResponse(responseCode = "200", description = "Counts response container object as JSON")
    @RequestMapping(value = "/conceptCounts", method = GET, produces = APPLICATION_JSON_VALUE)
    CountSearchResponse getCounts(@RequestParam UUID graphId) {
        logger.info("GET /conceptCounts requested");

        // fetch collections separately and add the count to dto because collections are not stored in elastic search
        JsonNode collectionList = termedService.getCollectionList(graphId);
        CountSearchResponse conceptCounts = elasticSearchService.getConceptCounts(graphId);
        conceptCounts.getCounts().getCategories().put(CountDTO.Category.COLLECTION.getName(), Long.valueOf(collectionList.size()));
        return conceptCounts;
    }

    @Operation(summary = "New version", description = "Creates new version of the terminology")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Terminology's uri and new prefix")
    @ApiResponse(responseCode = "200", description = "")
    @RequestMapping(value = "/createVersion", method = POST, produces = APPLICATION_JSON_VALUE)
    CreateVersionResponse createTerminologyVersion(@RequestBody CreateVersionDTO createVersionDTO) {

        logger.info("POST /createVersion requested");

        try {
            return termedService.createVersion(createVersionDTO);
        } catch (NamespaceInUseException | VocabularyNotFoundException e) {
            logger.error("Error creating new version", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unhandled error occurred while creating new version", e);
            throw new RuntimeException(e);
        }
    }
}
