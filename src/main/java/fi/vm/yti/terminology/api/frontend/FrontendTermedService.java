package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.exception.NamespaceInUseException;
import fi.vm.yti.terminology.api.exception.NodeNotFoundException;
import fi.vm.yti.terminology.api.exception.VocabularyNotFoundException;
import fi.vm.yti.terminology.api.frontend.searchdto.CreateVersionDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.CreateVersionResponse;
import fi.vm.yti.terminology.api.frontend.searchdto.StatusCountDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.StatusCountSearchResponse;
import fi.vm.yti.terminology.api.migration.DomainIndex;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import fi.vm.yti.terminology.api.util.JsonUtils;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static fi.vm.yti.security.AuthorizationException.check;
import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.TerminologicalVocabulary;
import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.Vocabulary;
import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToList;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Service
public class FrontendTermedService {

    private static final Logger logger = LoggerFactory.getLogger(FrontendTermedService.class);

    private static final String USER_PASSWORD = "user";
    private static final Pattern UUID_PATTERN = Pattern
            .compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Object USER_LOCK = new Object();

    private final TermedRequester termedRequester;
    private final FrontendGroupManagementService groupManagementService;
    private final AuthenticatedUserProvider userProvider;
    private final AuthorizationManager authorizationManager;
    private final String namespaceRoot;

    private final Cache<String, JsonNode> nodeListCache;

    @Autowired
    public FrontendTermedService(TermedRequester termedRequester, FrontendGroupManagementService groupManagementService,
            AuthenticatedUserProvider userProvider, AuthorizationManager authorizationManager,
            @Value("${namespace.root}") String namespaceRoot,
            @Value("${termed.cache.expiration:1800}") Long cacheExpireTime) {
        this.termedRequester = termedRequester;
        this.groupManagementService = groupManagementService;
        this.userProvider = userProvider;
        this.authorizationManager = authorizationManager;
        this.namespaceRoot = namespaceRoot;

        this.nodeListCache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheExpireTime, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();
    }

    public boolean isNamespaceInUse(String prefix) {

        String namespace = formatNamespace(prefix);

        for (Graph graph : getGraphs()) {
            if (prefix.equals(graph.getCode()) || namespace.equals(graph.getUri())) {
                return true;
            }
        }

        return false;
    }

    public @NotNull GenericNodeInlined getVocabulary(UUID graphId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "createdBy");
        params.add("select", "createdDate");
        params.add("select", "lastModifiedBy");
        params.add("select", "lastModifiedDate");
        params.add("select", "properties.*");
        params.add("select", "references.*");

        params.add("where", "graph.id:" + graphId + " AND (type.id:" + Vocabulary + " OR type.id:"
                + TerminologicalVocabulary + ")");

        params.add("max", "-1");
        addGraphTypeIds(graphId, params);

        List<GenericNodeInlined> result = requireNonNull(termedRequester.exchange("/node-trees", GET, params,
                new ParameterizedTypeReference<List<GenericNodeInlined>>() {
                }));

        if (result.isEmpty()) {
            throw new NodeNotFoundException(graphId, asList(NodeType.Vocabulary, NodeType.TerminologicalVocabulary));
        } else {
            GenericNodeInlined vocabulary = result.get(0);
            handleChildOrganizations(vocabulary);
            return userNameToDisplayName(vocabulary, new UserIdToDisplayNameMapper(),
                    authorizationManager.isUserPartOfOrganization(graphId));
        }
    }

    @NotNull JsonNode getVocabularyList(boolean incomplete) {

        if(userProvider.getUser() != null){ 
            logger.error("getVocabularyList: {}", userProvider.getUser().getUsername() );
        }
        List<UUID> orgList = new ArrayList<>();

        YtiUser user = userProvider.getUser();
        // Resolve current organizations for filtering
        Map<UUID,?> rolesAndOrgs = user.getRolesInOrganizations();
        rolesAndOrgs.forEach((k,v)-> orgList.add(k));
        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "properties.*");
        params.add("select", "references.contributor");
        params.add("select", "references.inGroup");

        params.add("where",
            "type.id:" +  TerminologicalVocabulary );
        params.add("max", "-1");
        // Execute full search
        JsonNode rv =  requireNonNull(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));
        // Super-user sees all
        if(user.isSuperuser()){
            return requireNonNull(rv);            
        }
        // normal users sees filtered
        List<JsonNode> nodes = new ArrayList<>();

        for(int x=0;x<rv.size();x++){
            JsonNode n = rv.get(x);
            UUID id=UUID.fromString(n.at("/references/contributor/0/id").textValue());
            String status=n.at("/properties/status/0/value").textValue();            
            if(status != null && 
               status.equalsIgnoreCase("INCOMPLETE")) {
                if(incomplete && 
                   orgList.contains(id)){
                    nodes.add(n);
                } else {
                    if(logger.isDebugEnabled()){ 
                        logger.debug("Dropped INCOMPLETE vocabulary {}", id);
                    }
                }
            } else {
                nodes.add(n);
            }  
        }
        rv = null;
        ObjectMapper mapper = new ObjectMapper();        
        return requireNonNull(mapper.convertValue(nodes,JsonNode.class));
    }

    public void createVocabulary(UUID templateGraphId, String prefix, GenericNode vocabularyNode, UUID graphId, boolean sync) {
        check(authorizationManager.canCreateVocabulary(vocabularyNode));

        //We have to set uri for vocabulary node by ourselves
        // otherwise termed will create a uri appending terminological-vocabulary-0
        vocabularyNode.setUri(namespaceRoot + prefix);

        try {
            handleChildOrganizations(vocabularyNode);

            var templateMetaNodes = getTypes(templateGraphId);
            var prefLabel = mapToList(vocabularyNode.getProperties().get("prefLabel"), Attribute::asProperty);

            logger.debug("Creating graph for \"{}\"", prefix);
            createGraph(prefix, prefLabel, graphId);
            logger.debug("Graph created for \"{}\"", prefix);
            var graphMetaNodes = mapToList(templateMetaNodes, node -> node.copyToGraph(graphId));

            logger.debug("Updating types for \"{}\"", prefix);
            updateTypes(graphId, graphMetaNodes);
            logger.debug("Handling nodes for \"{}\"", prefix);
            updateAndDeleteInternalNodes(
                    new GenericDeleteAndSave(emptyList(), singletonList(vocabularyNode.copyToGraph(graphId))), sync, null);
            logger.debug("Finished for \"{}\"", prefix);
        } catch (Exception e) {
            logger.error("Error occurred while creating terminology " + graphId, e);

            removeTypes(graphId, getTypes(graphId));
            deleteGraph(graphId);
            throw new RuntimeException(e);
        }
    }

    private void handleChildOrganizations(GenericNodeInlined vocabularyNode) {
        List<GenericNodeInlined> contributors = vocabularyNode.getReferences().get("contributor");

        JsonNode organizations = getOrganizations("fi", true);

        Set<GenericNodeInlined> parents = new HashSet<>();
        for (GenericNodeInlined contributor : contributors) {
            JsonNode org = StreamSupport.stream(organizations.spliterator(), false)
                    .filter(o -> o.get("id").asText().equals(contributor.getId().toString()))
                    .findFirst().orElseThrow();

            if (org.get("references").has("parent")) {
                JsonNode parent = org.get("references").get("parent").iterator().next();

                UUID id = UUID.fromString(parent.get("id").asText());
                if (contributors.stream().noneMatch(c -> c.getId().equals(id))) {
                    Iterator<JsonNode> labelsIterator = parent.get("properties").get("prefLabel").iterator();

                    List<Attribute> labels = new ArrayList<>();
                    while (labelsIterator.hasNext()) {
                        JsonNode label = labelsIterator.next();
                        labels.add(new Attribute(label.get("lang").asText(), label.get("value").asText()));
                    }
                    parents.add(new GenericNodeInlined(id,
                            "", "", 0L, "", new Date(), "", new Date(),
                            DomainIndex.ORGANIZATION_DOMAIN,
                            Map.of("prefLabel", labels),
                            emptyMap(),
                            emptyMap()
                    ));
                }
            }
        }
        vocabularyNode.getReferences().get("contributor").addAll(parents);
    }

    private void handleChildOrganizations(GenericNode vocabularyNode) {
        List<Identifier> contributors = vocabularyNode.getReferences().get("contributor");
        JsonNode organizations = getOrganizations("fi", true);

        Set<Identifier> parents = new HashSet<>();
        for (Identifier contributor : contributors) {
            JsonNode org = StreamSupport.stream(organizations.spliterator(), false)
                    .filter(o -> o.get("id").asText().equals(contributor.getId().toString()))
                    .findFirst().orElseThrow();

            if (org.get("references").has("parent")) {
                String parent = org.get("references").get("parent").iterator().next().get("id").asText();
                Identifier parentIdentifier = new Identifier(UUID.fromString(parent), DomainIndex.ORGANIZATION_DOMAIN);

                if (!contributors.contains(parentIdentifier)) {
                    parents.add(parentIdentifier);
                }
            }
        }
        vocabularyNode.getReferences().get("contributor").addAll(parents);
    }

    void deleteVocabulary(UUID graphId) {

        check(authorizationManager.canDeleteVocabulary(graphId));

        removeNodes(true, false, getAllNodeIdentifiers(Set.of(graphId)));
        removeTypes(graphId, getTypes(graphId));
        deleteGraph(graphId);
    }

    @NotNull
    public GenericNodeInlined getConcept(UUID graphId, UUID conceptId) {

        var params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "createdBy");
        params.add("select", "createdDate");
        params.add("select", "lastModifiedBy");
        params.add("select", "lastModifiedDate");
        params.add("select", "properties.*");
        params.add("select", "references.*");
        params.add("select", "references.prefLabelXl:2");
        params.add("select", "referrers.*");
        params.add("select", "referrers.prefLabelXl:2");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "id:" + conceptId);
        params.add("max", "-1");
        addGraphTypeIds(graphId, params);

        List<GenericNodeInlined> result = requireNonNull(termedRequester.exchange("/node-trees", GET, params,
                new ParameterizedTypeReference<List<GenericNodeInlined>>() {
                }));



        if (result.isEmpty()) {
            throw new NodeNotFoundException(graphId, conceptId);
        } else {
            var node = result.get(0);
            var isPartOfOrganization = authorizationManager.isUserPartOfOrganization(graphId);
            if(!isPartOfOrganization){
                node.getProperties().remove("editorialNote");
            }
            return userNameToDisplayName(node, new UserIdToDisplayNameMapper(),
                    isPartOfOrganization);
        }
    }

    @NotNull
    GenericNodeInlined getCollection(UUID graphId, UUID collectionId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "createdBy");
        params.add("select", "createdDate");
        params.add("select", "lastModifiedBy");
        params.add("select", "lastModifiedDate");
        params.add("select", "properties.*");
        params.add("select", "references.*");
        params.add("select", "references.prefLabelXl:2");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "id:" + collectionId);
        params.add("max", "-1");
        addGraphTypeIds(graphId, params);

        List<GenericNodeInlined> result = requireNonNull(termedRequester.exchange("/node-trees", GET, params,
                new ParameterizedTypeReference<List<GenericNodeInlined>>() {
                }));

        if (result.size() == 0) {
            throw new NodeNotFoundException(graphId, collectionId);
        } else {
            return userNameToDisplayName(result.get(0), new UserIdToDisplayNameMapper(),
                    authorizationManager.isUserPartOfOrganization(graphId));
        }
    }

    @NotNull
    JsonNode getCollectionList(UUID graphId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "properties.prefLabel");
        params.add("select", "properties.status");
        params.add("select", "properties.definition");
        params.add("select", "lastModifiedDate");
        params.add("select", "references.*");
        params.add("select", "references.prefLabelXl:2");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "type.id:" + "Collection");
        params.add("max", "-1");
        addGraphTypeIds(graphId, params);

        return requireNonNull(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));
    }

    public Long getCollectionCount(UUID graphId) {
        JsonNode collections = termedRequester.exchange(
                String.format("/graphs/%s/types/%s/nodes", graphId, NodeType.Collection),
                GET,
                new Parameters(),
                JsonNode.class);
        return collections != null ? Long.valueOf(collections.size()) : 0L;
    }

    public @NotNull List<GenericNode> getNodes(UUID graphId) {
        Parameters params = new Parameters();
        params.add("max", "-1");

        String path = graphId != null ? "/graphs/" + graphId + "/nodes" : "/nodes";

        return requireNonNull(
                termedRequester.exchange(path, GET, params, new ParameterizedTypeReference<List<GenericNode>>() {
                }));
    }

    public @NotNull GenericNode getConceptNode(UUID graphId, UUID conceptId) {
        Parameters params = new Parameters();
        params.add("max", "-1");
        String path = graphId != null ? "/graphs/" + graphId + "/types/Concept/nodes/" + conceptId : null;

        if (path == null)
            return null;
        return requireNonNull(
                termedRequester.exchange(path, GET, params, new ParameterizedTypeReference<GenericNode>() {
                }));
    }

    @NotNull
    public JsonNode getNodeListWithoutReferencesOrReferrers(NodeType nodeType) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "properties.*");
        params.add("where", "type.id:" + nodeType);
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));
    }

    JsonNode getOrganizations(String language, boolean showChildOrganizations) {
        JsonNode organizations = getNodeListWithoutReferrersV2(NodeType.Organization, language);
        if (!showChildOrganizations) {
            ObjectMapper mapper = new ObjectMapper();
            List<JsonNode> jsonNodeStream = StreamSupport.stream(organizations.spliterator(), false)
                    .filter(org -> !org.has("references")
                                   || !org.get("references").iterator().hasNext())
                    .collect(Collectors.toList());
            return mapper.valueToTree(jsonNodeStream);
        }
        return organizations;
    }

    @NotNull
    JsonNode getNodeListWithoutReferrersV2(NodeType nodeType, String language) {
        final String[] validLanguages = {"fi", "en", "sv"};

        if (!Arrays.asList(validLanguages).contains(language)) {
            language = "fi";
        }

        String key = nodeType.name() + language;

        JsonNode result = nodeListCache.getIfPresent(key);

        if (result != null) {
            return result;
        }

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "properties.*");
        params.add("select", "references.*");
        params.add("where", "type.id:" + nodeType);
        params.add("max", "-1");

        var init = requireNonNull(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));

        result = JsonUtils.sortedFromTermedProperties(init, language, validLanguages);
        nodeListCache.put(key, result);

        return result;
    }

    public void bulkChange(GenericDeleteAndSave deleteAndSave, boolean sync) {

        check(authorizationManager.canModifyNodes(deleteAndSave.getSave()));
        check(authorizationManager.canRemoveNodes(deleteAndSave.getDelete()));
        Optional<GenericNode> vocabularyNode = deleteAndSave.getSave()
                .stream()
                .filter(this::isVocabulary)
                .findFirst();

        vocabularyNode.ifPresent(this::handleChildOrganizations);

        updateAndDeleteInternalNodes(deleteAndSave, sync, null);
    }

    public void bulkChangeWithoutAuthorization(GenericDeleteAndSave deleteAndSave, boolean sync, UUID externalUserId) {
        updateAndDeleteInternalNodes(deleteAndSave, sync, externalUserId);
    }

    public void modifyStatuses(UUID graphId,
                             Set<String> types,
                             String oldStatus,
                             String newStatus) {
        check(authorizationManager.canModifyAllGraphs(singletonList(graphId)));

        Set<String> validTypes = Stream
                .of("Concept", "Term")
                .collect(Collectors.toSet());

        if (types.isEmpty() || !validTypes.containsAll(types)) {
            throw new IllegalArgumentException("Invalid types: " + String.join(", ", types));
        }

        Set<String> validStatuses = Stream
                .of("INCOMPLETE", "DRAFT", "VALID", "RETIRED", "SUPERSEDED")
                .collect(Collectors.toSet());

        if (!validStatuses.contains(oldStatus)) {
            throw new IllegalArgumentException("Invalid oldStatus: " + oldStatus);
        }
        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid newStatus: " + newStatus);
        }

        Parameters params = new Parameters();
        params.add("append", "false");

        // filter nodes to modify
        params.add("where", "properties.status:" + oldStatus);

        // specify new status with property values
        var properties = singletonMap(
                "properties",
                singletonMap(
                        "status",
                        singletonList(singletonMap(
                                "value",
                                newStatus
                        ))
                )
        );

        // need to make separate calls for each type
        for (var type : types) {
            termedRequester.exchange(
                    "/graphs/" + graphId + "/types/" + type + "/nodes",
                    HttpMethod.PATCH,
                    params,
                    String.class,
                    properties);
        }
    }

    void removeNodes(boolean sync, boolean disconnect, List<Identifier> identifiers) {

        check(authorizationManager.canRemoveNodes(identifiers));

        Parameters params = new Parameters();
        params.add("batch", "true");
        params.add("disconnect", Boolean.toString(disconnect));
        params.add("sync", Boolean.toString(sync));

        UUID username = ensureTermedUser(null);

        termedRequester.exchange("/nodes", HttpMethod.DELETE, params, String.class, identifiers, username.toString(),
                USER_PASSWORD);
    }

    public @NotNull List<MetaNode> getTypes(UUID graphId) {

        Parameters params = new Parameters();
        params.add("max", "-1");

        String path = graphId != null ? "/graphs/" + graphId + "/types" : "/types";

        return requireNonNull(
                termedRequester.exchange(path, GET, params, new ParameterizedTypeReference<List<MetaNode>>() {
                }));
    }

    public @NotNull List<Graph> getGraphs() {

        Parameters params = new Parameters();
        params.add("max", "-1");

        return requireNonNull(
                termedRequester.exchange("/graphs", GET, params, new ParameterizedTypeReference<List<Graph>>() {
                }));
    }

    public @NotNull Graph getGraph(UUID graphId) {
        return requireNonNull(termedRequester.exchange("/graphs/" + graphId, GET, Parameters.empty(), Graph.class));
    }

    public CreateVersionResponse createVersion(CreateVersionDTO createVersionDTO) throws Exception {
        logger.info("Creating new version from vocabulary {}. New prefix {}",
                createVersionDTO.getGraphId(), createVersionDTO.getNewCode());

        check(authorizationManager.canCreateNewVersion(createVersionDTO.getGraphId()));

        if (this.isNamespaceInUse(createVersionDTO.getNewCode())) {
            throw new NamespaceInUseException();
        }

        Dump dump = termedRequester.exchange("/graphs/" + createVersionDTO.getGraphId() + "/dump",
                GET, Parameters.empty(), Dump.class);

        if (dump == null || dump.getGraphs().isEmpty()) {
            throw new VocabularyNotFoundException(createVersionDTO.getGraphId());
        }

        Graph oldGraph = dump.getGraphs().get(0);
        UUID newGraphId = UUID.randomUUID();

        Graph graph = new Graph(newGraphId,
                createVersionDTO.getNewCode(),
                formatNamespace(createVersionDTO.getNewCode()),
                oldGraph.getRoles(),
                oldGraph.getPermissions(),
                oldGraph.getProperties());

        List<MetaNode> metaNodes = dump.getTypes().stream().map(t -> new MetaNode(
                t.getId(),
                t.getUri(),
                t.getIndex(),
                t.getGraph(),
                t.getPermissions(),
                t.getProperties(),
                t.getTextAttributes(),
                t.getReferenceAttributes())
            .copyToGraph(newGraphId)
        ).collect(Collectors.toList());

        // Create id map for saving references
        Map<UUID, UUID> nodeIdMap = new HashMap<>();
        dump.getNodes().stream().forEach(n -> nodeIdMap.put(n.getId(), UUID.randomUUID()));

        List<GenericNode> nodes = dump.getNodes().stream().map(n -> new GenericNode(
                nodeIdMap.get(n.getId()),
                n.getCode(),
                String.format("%s%s",
                        formatNamespace(createVersionDTO.getNewCode()), n.getCode()),
                n.getNumber(),
                n.getCreatedBy(),
                n.getCreatedDate(),
                n.getLastModifiedBy(),
                n.getLastModifiedDate(),
                n.getType(),
                getNewVersionProperties(n),
                n.getReferences(),
                n.getReferrers())
            .copyAllToGraph(newGraphId, nodeIdMap)
        ).collect(Collectors.toList());

        Dump newVersion = new Dump(asList(graph), metaNodes, nodes);

        try {
            UUID username = ensureTermedUser(null);
            termedRequester.exchange("/dump", POST, Parameters.empty(), String.class,
                    newVersion, username.toString(), USER_PASSWORD);
        } catch (Exception e) {
            logger.error("Error creating new version", e);

            try {
                // If an error occurs, graph has been created in some cases. Try to delete that
                removeTypes(newGraphId, getTypes(newGraphId));
                deleteGraph(newGraphId);
            } catch (Exception ex) {
                logger.error("Cannot delete graph " + newGraphId, ex);
            }

            throw e;
        }

        return new CreateVersionResponse(newGraphId, formatNamespace(createVersionDTO.getNewCode()));
    }

    public void flushCache() {
        // for now only for unit tests
        nodeListCache.invalidateAll();
    }

    private Map<String, List<Attribute>> getNewVersionProperties(GenericNode node) {
        Map<String, List<Attribute>> properties = new HashMap<>();
        var originalProperties = node.getProperties();

        originalProperties.keySet().stream().forEach(key -> {

            var originalAttributes = originalProperties.get(key);

            // change all statuses to DRAFT
            if ("status".equals(key)) {
                var newAttributes = originalAttributes.stream().map(att ->
                   new Attribute(att.getLang(), "DRAFT", null)
                ).collect(Collectors.toList());

                properties.put(key, newAttributes);
            } else if ("prefLabel".equals(key) && isVocabulary(node)) {
                var newAttributes = originalAttributes.stream().map(att ->
                        new Attribute(att.getLang(), att.getValue() + " (Copy)", null)
                ).collect(Collectors.toList());

                properties.put(key, newAttributes);
            } else {
                properties.put(key, originalAttributes);
            }
        });

        // store origin of new version
        if (isVocabulary(node)) {
            properties.put("origin", List.of(new Attribute("", node.getUri())));
        }

        return properties;
    }

    private boolean isVocabulary(Node node) {
        return asList(NodeType.TerminologicalVocabulary, NodeType.Vocabulary)
                .contains(node.getType().getId());
    }

    public @NotNull List<Identifier> getAllNodeIdentifiers(Set<UUID> graphIds) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("where", "graph.id:" + graphIds.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(" OR graph.id:"))
        );
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange("/node-trees", GET, params,
                new ParameterizedTypeReference<List<Identifier>>() {
                }));
    }

    private void createGraph(String prefix, List<Property> prefLabel, UUID graphId) {

        String code = prefix;
        String uri = formatNamespace(prefix);
        List<String> roles = emptyList();
        Map<String, List<Permission>> permissions = emptyMap();
        Map<String, List<Property>> properties = singletonMap("prefLabel", prefLabel);

        Graph graph = new Graph(graphId, code, uri, roles, permissions, properties);

        termedRequester.exchange("/graphs", POST, Parameters.single("sync", "true"), String.class, graph);
    }

    private void updateAndDeleteInternalNodes(GenericDeleteAndSave deleteAndSave, boolean sync, UUID externalUserId) {

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", String.valueOf(sync));

        UUID username = ensureTermedUser(externalUserId);

        this.termedRequester.exchange("/nodes", POST, params, String.class, deleteAndSave, username.toString(),
                USER_PASSWORD);
    }

    private void deleteGraph(UUID graphId) {
        termedRequester.exchange("/graphs/" + graphId, HttpMethod.DELETE, Parameters.empty(), String.class);
    }

    private void updateTypes(UUID graphId, List<MetaNode> metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");
        params.add("sync", "true");

        termedRequester.exchange("/graphs/" + graphId + "/types", POST, params, String.class, metaNodes);
    }

    private void removeTypes(UUID graphId, List<MetaNode> metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        termedRequester.exchange("/graphs/" + graphId + "/types", HttpMethod.DELETE, params, String.class, metaNodes);
    }

    public UUID ensureTermedUser(UUID externalUserId) {

        YtiUser user = userProvider.getUser();

        if (user.isAnonymous() && externalUserId == null) {
            throw new RuntimeException("Logged in user needed for the operation");
        }

        if (findTermedUser(user) == null) {
            synchronized (USER_LOCK) {
                if (findTermedUser(user) == null) {
                    createTermedUser(user, externalUserId);
                }
            }
        }

        if (externalUserId != null) {
            return externalUserId;
        } else {
            return user.getId();
        }
    }

    private @Nullable TermedUser findTermedUser(YtiUser user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        Parameters params = Parameters.single("username", user.getId().toString());
        return termedRequester.exchange("/users", GET, params, TermedUser.class);
    }

    private void createTermedUser(YtiUser user, UUID externalUserId) {
        Parameters params = Parameters.single("sync", "true");
        String userIdForTermedUser = externalUserId == null ? user.getId().toString() : externalUserId.toString();
        TermedUser termedUser = new TermedUser(userIdForTermedUser, USER_PASSWORD, "ADMIN");
        logger.info("Creating termed user for: {}", userIdForTermedUser);
        termedRequester.exchange("/users", POST, params, String.class, termedUser);
    }

    private String formatNamespace(@NotNull String prefix) {
        return this.namespaceRoot + prefix + '/';
    }

    private GenericNodeInlined userNameToDisplayName(GenericNodeInlined node,
                                                     UserIdToDisplayNameMapper userIdToDisplayNameMapper,
                                                     boolean mapUserNames) {
        return new GenericNodeInlined(node.getId(), node.getCode(), node.getUri(), node.getNumber(),
                mapUserNames ? userIdToDisplayNameMapper.map(node.getCreatedBy()) : null, node.getCreatedDate(),
                mapUserNames ? userIdToDisplayNameMapper.map(node.getLastModifiedBy()) : null, node.getLastModifiedDate(),
                node.getType(), node.getProperties(),
                mapMapValues(node.getReferences(), x -> userNameToDisplayName(x, userIdToDisplayNameMapper, mapUserNames)),
                mapMapValues(node.getReferrers(), x -> userNameToDisplayName(x, userIdToDisplayNameMapper, mapUserNames)));
    }

    private GenericNode userNameToDisplayName(GenericNode node, UserIdToDisplayNameMapper userIdToDisplayNameMapper) {

        return new GenericNode(node.getId(), node.getCode(), node.getUri(), node.getNumber(),
                userIdToDisplayNameMapper.map(node.getCreatedBy()), node.getCreatedDate(),
                userIdToDisplayNameMapper.map(node.getLastModifiedBy()), node.getLastModifiedDate(), node.getType(),
                node.getProperties(), node.getReferences(), node.getReferrers());
    }

    private static <K, V> Map<K, List<V>> mapMapValues(Map<K, List<V>> map, Function<V, V> mapper) {
        return map.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> mapToList(e.getValue(), mapper)));
    }

    private static boolean isUUID(String s) {
        return UUID_PATTERN.matcher(s).matches();
    }

    private static void addGraphTypeIds(UUID graphId, Parameters params) {
        params.add("graphTypeId", graphId.toString());
        params.add("graphTypeId", DomainIndex.ORGANIZATION_DOMAIN.getGraphId().toString());
        params.add("graphTypeId", DomainIndex.GROUP_DOMAIN.getGraphId().toString());
    }

    // XXX consider cache spanning to multiple requests
    private class UserIdToDisplayNameMapper {

        private final Map<String, String> cache = new HashMap<>();

        private String map(String userId) {
            return cache.computeIfAbsent(userId, this::mapRemote);
        }

        private String mapRemote(String userId) {

            if (!isUUID(userId)) {
                return "";
            }

            GroupManagementUser user = groupManagementService.findUser(userId);

            if (user != null) {
                return user.getDisplayName();
            } else {
                return "";
            }
        }
    }

    @NotNull
    StatusCountSearchResponse getConceptTermsCount(UUID graphId) {
        StatusCountSearchResponse response = new StatusCountSearchResponse();

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "properties.status");
        params.add("select", "references.prefLabelXl");
        params.add("select", "references.altLabelXl");
        params.add("select", "references.notRecommendedSynonym");
        params.add("select", "references.searchTerm");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "type.id:Concept");
        params.add("max", "-1");
        addGraphTypeIds(graphId, params);

        List<GenericNodeInlined> result = requireNonNull(termedRequester.exchange("/node-trees", GET, params,
                new ParameterizedTypeReference<List<GenericNodeInlined>>() {
                }));

        if (result.isEmpty()) {
            throw new NodeNotFoundException(graphId, asList(NodeType.Vocabulary, NodeType.TerminologicalVocabulary));
        } else {
            Map<String, Long> conceptStatuses = Stream.of(new Object[][] {
                    {"DRAFT", 0L},
                    {"RETIRED", 0L},
                    {"SUPERSEDED", 0L},
                    {"VALID", 0L}
            }).collect(Collectors.toMap(data -> (String) data[0], data -> (Long) data[1]));
            Map<String, Long> termStatuses = Stream.of(new Object[][] {
                    {"DRAFT", 0L},
                    {"RETIRED", 0L},
                    {"SUPERSEDED", 0L},
                    {"VALID", 0L}
            }).collect(Collectors.toMap(data -> (String) data[0], data -> (Long) data[1]));

            result.forEach(r -> {
                String conceptStatus = r.getProperties().get("status").get(0).getValue();
                if (conceptStatuses.containsKey(conceptStatus)) {
                    conceptStatuses.replace(conceptStatus, conceptStatuses.get(conceptStatus) + 1L);
                }

                List<GenericNodeInlined> terms = new ArrayList<>();

                if (r.getReferences().containsKey("prefLabelXl")) {
                    terms.addAll(r.getReferences().get("prefLabelXl"));
                }
                if (r.getReferences().containsKey("altLabelXl")) {
                    terms.addAll(r.getReferences().get("altLabelXl"));
                }
                if (r.getReferences().containsKey("notRecommendedSynonym")) {
                    terms.addAll(r.getReferences().get("notRecommendedSynonym"));
                }
                if (r.getReferences().containsKey("searchTerm")) {
                    terms.addAll(r.getReferences().get("searchTerm"));
                }

                terms.forEach(label -> {
                    String status = label.getProperties().get("status").get(0).getValue();
                    if (termStatuses.containsKey(status)) {
                        termStatuses.replace(status, termStatuses.get(status) + 1L);
                    }
                });
            });

            response.setCounts(new StatusCountDTO(conceptStatuses, termStatuses));
        }

        return response;
    }
}
