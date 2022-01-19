package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import fi.vm.yti.terminology.api.util.Parameters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Import({
        FrontendTermedService.class
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

    final JsonNode initGroupsNode = mapper.readTree(groupsJsonData);
    final JsonNode initOrgsNode = mapper.readTree(organizationsJsonData);

    FrontendTermedServiceTest() throws JsonProcessingException {
    }

    @Test
    public void testReturnsGroupsUnordered() {
        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrers(NodeType.Group, Optional.empty());

        assertEquals(initGroupsNode, gotten);
    }

    @Test
    public void testReturnsGroupsUnorderedInUnknownLanguage() {
        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrers(NodeType.Group, Optional.of("random_string"));

        assertEquals(initGroupsNode, gotten);
    }

    @Test
    public void testReturnsGroupsOrderedInFi() throws JsonProcessingException {
        String jsonData = "[" +
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
                "]}}," +
                "{\"id\": \"123\"," +
                "\"properties\": " +
                "{ \"prefLabel\": [" +
                "{ \"lang\": \"en\", \"value\": \"Services for families\"}," +
                "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\"}" +
                "]}} " +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrers(NodeType.Group, Optional.of("fi"));

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsGroupsOrderedInEn() throws JsonProcessingException {
        String jsonData = "[" +
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
                "]}}," +
                "{\"id\": \"123\"," +
                "\"properties\": " +
                "{ \"prefLabel\": [" +
                "{ \"lang\": \"en\", \"value\": \"Services for families\"}," +
                "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\"}" +
                "]}} " +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrers(NodeType.Group, Optional.of("en"));

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsUnordered() {
        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrers(NodeType.Organization, Optional.empty());

        assertEquals(initOrgsNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsUnorderedInUnknownLanguages() {
        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrers(NodeType.Organization, Optional.of("random_string"));

        assertEquals(initOrgsNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsOrderedInFi() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"654\"," +
                "\"properties\": " +
                "{ \"prefLabel\": [" +
                "{ \"lang\": \"en\", \"value\": \"Test-organization\"}," +
                "{ \"lang\": \"fi\", \"value\": \"Testi-organisaatio\"}" +
                "]}}," +
                "{\"id\": \"321\"," +
                "\"properties\": " +
                "{ \"prefLabel\": [" +
                "{ \"lang\": \"en\", \"value\": \"Interoperability platform developers\"}," +
                "{ \"lang\": \"fi\", \"value\": \"Yhteentoimivuusalustan yllapito\"}" +
                "]}}" +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrers(NodeType.Organization, Optional.of("fi"));

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsOrderedInEn() throws JsonProcessingException {
        String jsonData = "[" +
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
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrers(NodeType.Organization, Optional.of("en"));

        assertEquals(expectedNode, gotten);
    }
}
