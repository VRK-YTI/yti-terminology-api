package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.AuthorizationException;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.exception.NamespaceInUseException;
import fi.vm.yti.terminology.api.exception.VocabularyNotFoundException;
import fi.vm.yti.terminology.api.frontend.searchdto.CreateVersionDTO;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import fi.vm.yti.terminology.api.util.Parameters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static java.util.Collections.*;
import static java.util.Arrays.*;

@ExtendWith(SpringExtension.class)
@Import({
        FrontendTermedService.class
})
@TestPropertySource(properties = {
        "namespace.root=http://uri.suomi.fi/terminology/"
})
class FrontendTermedServiceTest {

    @MockBean
    FrontendGroupManagementService groupManagementService;

    @MockBean
    AuthorizationManager authorizationManager;

    @MockBean
    AuthenticatedUserProvider authenticatedUserProvider;

    @MockBean
    TermedRequester termedRequester;

    @Autowired
    FrontendTermedService frontEndTermedService;

    @Captor
    ArgumentCaptor<Dump> dumpCaptor;

    @Captor
    ArgumentCaptor<String> stringCaptor;

    ObjectMapper mapper = new ObjectMapper();

    final String groupsJsonData = "[" +
            "{\"id\": \"123\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Services for families\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\"}" +
            "]}}," +
            "{\"id\": \"789\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Housing\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Asuminen\"}" +
            "]}}," +
            "{\"id\": \"456\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Public order\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Järjestys\"}" +
            "]}} " +
            "]";

    final String groupsJsonMissingData = "[" +
            "{\"id\": \"123\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Services for families\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\"}" +
            "]}}," +
            "{\"id\": \"789\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Housing\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Asuminen\"}" +
            "]}}," +
            "{\"id\": \"456\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Public order\"}" +
            "]}} " +
            "]";

    final String organizationsJsonData = "[" +
            "{\"id\": \"321\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Interoperability platform developers\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Yhteentoimivuusalustan yllapito\"}" +
            "]}}," +
            "{\"id\": \"654\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Test-organization\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Testi-organisaatio\"}" +
            "]}}" +
            "]";

    final String organizationsJsonMissingData = "[" +
            "{\"id\": \"321\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Interoperability platform developers\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Yhteentoimivuusalustan yllapito\"}" +
            "]}}," +
            "{\"id\": \"654\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"fi\", \"value\": \"Testi-organisaatio\"}" +
            "]}}" +
            "]";

    final JsonNode initGroupsNode = mapper.readTree(groupsJsonData);
    final JsonNode initGroupsNodeMissing = mapper.readTree(groupsJsonMissingData);
    final JsonNode initOrgsNode = mapper.readTree(organizationsJsonData);
    final JsonNode initOrgsNodeMissing = mapper.readTree(organizationsJsonMissingData);

    FrontendTermedServiceTest() throws JsonProcessingException {
    }

    @Test
    public void testReturnsGroupsOrderedInFiWhenUnknownLanguage() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"789\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Asuminen\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"456\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Järjestys\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"123\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\", \"regex\":\"(?s)^.*$\"}" +
                "}} " +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Group, "random_string");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsGroupsOrderedInFi() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"789\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Asuminen\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"456\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Järjestys\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"123\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\", \"regex\":\"(?s)^.*$\"}" +
                "}} " +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Group, "fi");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsGroupsOrderedInEn() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"789\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Housing\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"456\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Public order\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"123\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Services for families\", \"regex\":\"(?s)^.*$\"}" +
                "}} " +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Group, "en");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsOrderedInFiWhenUnknownLanguages() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"654\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Testi-organisaatio\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"321\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Yhteentoimivuusalustan yllapito\", \"regex\":\"(?s)^.*$\"}" +
                "}}" +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Organization, "random_string");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsOrderedInFi() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"654\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Testi-organisaatio\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"321\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Yhteentoimivuusalustan yllapito\", \"regex\":\"(?s)^.*$\"}" +
                "}}" +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Organization, "fi");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsOrderedInEn() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"321\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Interoperability platform developers\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"654\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Test-organization\", \"regex\":\"(?s)^.*$\"}" +
                "}}" +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Organization, "en");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsOrderedWhenMissingLang() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"321\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Interoperability platform developers\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"654\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Testi-organisaatio (fi)\", \"regex\":\"(?s)^.*$\"}" +
                "}}" +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNodeMissing);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Organization, "en");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsGroupsOrderedWhenMissingLang() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"789\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Asuminen\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"123\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"456\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Public order (en)\", \"regex\":\"(?s)^.*$\"}" +
                "}} " +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNodeMissing);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Organization, "fi");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void modifyValidStatusesShouldSucceed() {
        var graphId = UUID.fromString("0973d8f3-a129-4545-8d0d-d452e8acbc55");
        var oldStatus = "DRAFT";

        doReturn(true)
                .when(this.authorizationManager)
                .canModifyAllGraphs(any());

        frontEndTermedService.modifyStatuses(
                graphId,
                new HashSet<>(Arrays.asList("Concept", "Term")),
                oldStatus,
                "VALID");

        ArgumentCaptor<Map<String, Map<String, List<Map<String, String>>>>> propertiesCaptor =
                ArgumentCaptor.forClass(Map.class);

        // verify termed call to patch Concepts
        verify(this.termedRequester).exchange(
                eq("/graphs/" + graphId + "/types/Concept/nodes"),
                eq(HttpMethod.PATCH),
                argThat(i -> i.toString().equals(
                        "?append=false&where=properties.status:" + oldStatus)),
                eq(String.class),
                propertiesCaptor.capture());

        // verify termed call to patch Terms
        verify(this.termedRequester).exchange(
                eq("/graphs/" + graphId + "/types/Term/nodes"),
                eq(HttpMethod.PATCH),
                argThat(i -> i.toString().equals(
                        "?append=false&where=properties.status:" + oldStatus)),
                eq(String.class),
                propertiesCaptor.capture());

        // verify properties sent to termed
        var capturedProperties = propertiesCaptor
                .getAllValues()
                .stream()
                .map(properties -> properties
                        .get("properties")
                        .get("status")
                        .get(0)
                        .get("value"))
                .collect(Collectors.toList())
                .toArray();
        assertArrayEquals(
                new String[] { "VALID", "VALID" },
                capturedProperties);
    }

    @ParameterizedTest
    @CsvSource({ "FOO,VALID", "VALID,FOO" })
    public void modifyInvalidStatusesShouldFail(String oldStatus, String newStatus) {
        var graphId = UUID.fromString("0973d8f3-a129-4545-8d0d-d452e8acbc55");

        doReturn(true)
                .when(this.authorizationManager)
                .canModifyAllGraphs(any());

        var ex = assertThrows(
                IllegalArgumentException.class, () ->
                        frontEndTermedService.modifyStatuses(
                                graphId,
                                new HashSet<>(Arrays.asList("Concept", "Term")),
                                oldStatus,
                                newStatus));

        assertTrue(ex.getMessage().matches("^Invalid (old|new)Status: FOO$"));

        verify(this.termedRequester, times(0)).exchange(
                any(),
                any(),
                any(),
                eq(String.class),
                anyMap());
    }

    @Test
    public void modifyInvalidTypesShouldFail() {
        var graphId = UUID.fromString("0973d8f3-a129-4545-8d0d-d452e8acbc55");

        doReturn(true)
                .when(this.authorizationManager)
                .canModifyAllGraphs(any());

        var ex = assertThrows(
                IllegalArgumentException.class, () ->
                        frontEndTermedService.modifyStatuses(
                                graphId,
                                new HashSet<>(Arrays.asList("Concept", "Foo")),
                                "DRAFT",
                                "VALID"));
        assertEquals("Invalid types: Concept, Foo", ex.getMessage());

        verify(this.termedRequester, times(0)).exchange(
                any(),
                any(),
                any(),
                eq(String.class),
                anyMap());
    }

    @Test
    public void testCreateVersionNotAuthenticated() {
        assertThrows(AuthorizationException.class, () -> {
            var dto = new CreateVersionDTO(UUID.randomUUID(), "some_vocabulary");
            frontEndTermedService.createVersion(dto);
        });
    }

    @Test
    public void testCreateVersionNamespaceAlreadyInUse() throws Exception {
        mockTermedGetGraphs();
        mockAuthorization();

        assertThrows(NamespaceInUseException.class, () -> {
            var dto = new CreateVersionDTO(UUID.randomUUID(), "test");
            frontEndTermedService.createVersion(dto);
        });
    }

    @Test
    public void testCreateVersionVocabularyNotFound() {
        mockTermedGetGraphs();
        mockAuthorization();

        assertThrows(VocabularyNotFoundException.class, () -> {
            var dto = new CreateVersionDTO(UUID.randomUUID(), "test_v2");
            frontEndTermedService.createVersion(dto);
        });
    }

    @Test
    public void testCreateNewVersion() throws Exception {
        UUID vocabularyId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockTermedGetGraphs();
        mockAuthorization();
        mockUser(userId);
        mockTermedGetDump(new GraphId(vocabularyId), "prefix", organizationId);

        var dto = new CreateVersionDTO(vocabularyId, "prefix_v2");

        var versionResponse = frontEndTermedService.createVersion(dto);

        verify(termedRequester).exchange(
                eq("/dump"),
                eq(HttpMethod.POST),
                any(Parameters.class),
                eq(String.class),
                dumpCaptor.capture(),
                stringCaptor.capture(),
                anyString());

        // Modified new version data sent to termed-api
        Dump dump = dumpCaptor.getValue();
        String requestUserId = stringCaptor.getValue();

        Graph graph = dump.getGraphs().get(0);
        String newUri = "http://uri.suomi.fi/terminology/" + dto.getNewCode() + "/";

        assertEquals(userId.toString(), requestUserId);

        assertEquals(newUri, versionResponse.getUri());

        assertEquals(dto.getNewCode(), graph.getCode());
        assertEquals(newUri, graph.getUri());

        // graph with new id is created
        assertNotEquals(graph.getId(), vocabularyId);

        MetaNode metaNode = dump.getTypes().get(0);
        assertEquals(graph.getId(), metaNode.getGraph().getId());
        assertEquals("Concept", metaNode.getId());
        assertEquals("http://www.w3.org/concept", metaNode.getUri());
        assertEquals("metanode_prop", metaNode.getProperties().get("prop").get(0).getValue());

        List<GenericNode> nodes = dump.getNodes();

        // each node should have status = DRAFT and they should belong to new graph
        nodes.forEach(n -> n.getProperties()
                .getOrDefault("status", new ArrayList<>())
                .stream()
                .forEach(p -> {
                    assertEquals("DRAFT", p.getValue());
                    assertEquals(graph.getId(), n.getType().getGraphId());
                }));

        var vocabularyNode = getNodeByType(nodes, NodeType.TerminologicalVocabulary);
        var conceptNode = getNodeByType(nodes, NodeType.Concept);
        var termNode = getNodeByType(nodes, NodeType.Term);
        var collectionNode = getNodeByType(nodes, NodeType.Collection);

        // add new property 'origin' to vocabulary node
        assertEquals(getUri("prefix", "vocabulary-1234"),
                vocabularyNode.getProperties().get("origin").get(0).getValue());

        // add suffix to terminology name
        assertTrue(vocabularyNode.getProperties().get("prefLabel").get(0).getValue().endsWith(" (Copy)"));

        // contributor's id remains the same and it should have its own graph id
        Identifier contributor = vocabularyNode.getReferences().get("contributor").get(0);
        assertNotEquals(graph.getId(), contributor.getType().getGraphId());
        assertEquals(organizationId, contributor.getId());

        // codes remain the same
        assertEquals("vocabulary-1234", vocabularyNode.getCode());
        assertEquals("concept-1234", conceptNode.getCode());
        assertEquals("term-1234", termNode.getCode());

        // uris contain new prefix
        assertEquals(getUri(dto.getNewCode(), vocabularyNode), vocabularyNode.getUri());
        assertEquals(getUri(dto.getNewCode(), conceptNode), conceptNode.getUri());
        assertEquals(getUri(dto.getNewCode(), termNode), termNode.getUri());

        // term and concept refer still each other
        assertEquals(conceptNode.getReferences().get("prefLabelXl").get(0).getId(),
                termNode.getId());
        assertEquals(termNode.getReferrers().get("prefLabelXl").get(0).getId(),
                conceptNode.getId());

        assertEquals(conceptNode.getId(), collectionNode.getReferences().get("member").get(0).getId());
    }

    private String getUri(String prefix, String code) {
        return "http://uri.suomi.fi/terminology/" + prefix + "/" + code + "/";
    }

    private String getUri(String prefix, GenericNode node) {
        return getUri(prefix, node.getCode());
    }

    private GenericNode getNodeByType(List<GenericNode> nodes, NodeType nodeType) {
        return nodes.stream()
                .filter(n -> n.getType().getId().equals(nodeType))
                .findFirst()
                .get();
    }

    private void mockAuthorization() {
        when(authorizationManager.canCreateNewVersion(any(UUID.class))).thenReturn(true);
    }

    private void mockUser(UUID userId) {
        when(authenticatedUserProvider.getUser())
                .thenReturn(new YtiUser(
                        "admin@localhost",
                        "Admin",
                        "Test",
                        userId,
                        false,
                        false,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        emptyMap(),
                        null,
                        null
                ));
    }

    private void mockTermedGetGraphs(Graph... graphs) {
        var defaultGraph = new Graph(UUID.randomUUID(), "test", "http://uri.suomi.fi/test", emptyList(), emptyMap(), emptyMap());

        var response = asList(defaultGraph);
        response.addAll(asList(graphs));

        when(termedRequester.exchange(
                eq("/graphs"),
                eq(HttpMethod.GET),
                any(Parameters.class),
                any(ParameterizedTypeReference.class)))
                    .thenReturn(response);
    }

    private void mockTermedGetDump(GraphId graphId, String code, UUID orgId) {

        var organizationGraphId = new GraphId(UUID.randomUUID());

        var graph = new Graph(
                graphId.getId(),
                code,
                "http://uri.suomi.fi/terminology/" + code,
                emptyList(),
                emptyMap(),
                emptyMap());

        var metaNode = new MetaNode(
                "Concept",
                "http://www.w3.org/concept",
                1L,
                graphId,
                emptyMap(),
                Map.of("prop", asList(new Property("fi", "metanode_prop"))),
                emptyList(),
                emptyList());

        var organizationIdentifier = new Identifier(
                orgId,
                new TypeId(
                        NodeType.Organization,
                        organizationGraphId)
        );
        var termIdentifier = new Identifier(
                UUID.randomUUID(),
                new TypeId(
                        NodeType.Term,
                        graphId)
        );
        var conceptIdentifier = new Identifier(
                UUID.randomUUID(),
                new TypeId(
                        NodeType.Concept,
                        graphId));

        var vocabularyNode = new GenericNode(
                UUID.randomUUID(),
                "vocabulary-1234",
                getUri(code, "vocabulary-1234"),
                1L,
                "creator_user",
                new Date(),
                "modifier_user",
                new Date(),
                new TypeId(NodeType.TerminologicalVocabulary, graphId, null),
                Map.of(
                        "prefLabel", asList(new Attribute("fi", "value")),
                        "status", asList(new Attribute("fi", "VALID"))),
                Map.of("contributor", asList(organizationIdentifier)),
                emptyMap()
        );

        var conceptNode = new GenericNode(
                conceptIdentifier.getId(),
                "concept-1234",
                getUri(code, "concept-1234"),
                1L,
                "creator_user",
                new Date(),
                "modifier_user",
                new Date(),
                new TypeId(NodeType.Concept, graphId),
                Map.of("status", asList(new Attribute("fi", "DRAFT"))),
                Map.of("prefLabelXl", asList(termIdentifier)),
                emptyMap()
        );

        var termNode = new GenericNode(
                termIdentifier.getId(),
                "term-1234",
                getUri(code, "term-1234"),
                1L,
                "creator_user",
                new Date(),
                "modifier_user",
                new Date(),
                new TypeId(NodeType.Term, graphId),
                Map.of(
                        "prefLabel", asList(new Attribute("fi", "value")),
                        "status", asList(new Attribute("fi", "VALID"))),
                emptyMap(),
                Map.of("prefLabelXl", asList(conceptIdentifier))
        );

        var collectionNode = new GenericNode(
                UUID.randomUUID(),
                "collection-1234",
                getUri(code, "collection-1234"),
                1L,
                "creator_user",
                new Date(),
                "modifier_user",
                new Date(),
                new TypeId(NodeType.Collection, graphId),
                Map.of("prefLabel", asList(new Attribute("fi", "value"))),
                Map.of("member", asList(conceptIdentifier)),
                emptyMap());

        when(termedRequester.exchange(
                eq("/graphs/" + graphId.getId() + "/dump"),
                eq(HttpMethod.GET),
                any(Parameters.class),
                eq(Dump.class)))
                    .thenReturn(new Dump(
                            asList(graph),
                            asList(metaNode),
                            asList(vocabularyNode, conceptNode, termNode, collectionNode)
                    ));
    }
}
