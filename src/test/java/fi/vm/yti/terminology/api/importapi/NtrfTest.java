package fi.vm.yti.terminology.api.importapi;

import com.google.common.io.CharStreams;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.model.ntrf.VOCABULARY;

import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static java.util.Collections.*;
import static java.util.Arrays.*;

@ExtendWith(SpringExtension.class)
@Import({
        NtrfMapper.class
})
public class NtrfTest {

    @MockBean
    TermedRequester termedRequester;

    @MockBean
    FrontendTermedService termedService;

    @MockBean
    AuthenticatedUserProvider userProvider;

    @MockBean
    YtiMQService ytiMQService;

    @Autowired
    NtrfMapper mapper;

    @Captor
    ArgumentCaptor<GenericDeleteAndSave> deleteAndSaveArgumentCaptor;

    @BeforeEach
    public void setUp() {
        mockUser();
        mockTermedTypes();
        mockTermedGetGraph();
    }

    @Test
    public void testMapTermInfo() throws Exception {
        VOCABULARY vocabulary = NtrfUtil.unmarshallXmlDocument(getContent("term-and-concept-info.xml"));

        mapper.mapNtrfDocument("xyz", UUID.randomUUID(), vocabulary, UUID.randomUUID());

        verify(termedRequester, times(2)).exchange(
                eq("/nodes"),
                eq(HttpMethod.POST),
                any(Parameters.class),
                eq(String.class),
                deleteAndSaveArgumentCaptor.capture(),
                anyString(),
                anyString());

        var nodes = deleteAndSaveArgumentCaptor.getAllValues().get(0).getSave();

        var concept = getConceptNode(nodes, "c100");
        var recommendedTerm = getTerm(concept, nodes, "prefLabelXl");
        var synonym = getTerm(concept, nodes, "altLabelXl");
        var searchTerm = getTerm(concept, nodes, "searchTerm");

        // Concept properties
        assertEquals("Käsitteen luokka", getPropertyValue(concept, "conceptClass"));
        assertEquals("Käyttöala", getPropertyValue(concept, "conceptScope"));
        assertEquals(Arrays.asList("Huomautus 1", "Huomautus 2"), getPropertyValues(concept, "note"));
        assertEquals("Käsitteen määritelmä", getPropertyValue(concept, "definition"));
        assertEquals(Arrays.asList(" - Viimeksi muokattu, 2022-03-30", "Editorial note concept"),
                getPropertyValues(concept, "editorialNote"));
        assertEquals("Lähde", getPropertyValue(concept, "source"));
        assertEquals(Status.VALID.name(), getPropertyValue(concept, "status"));

        // Concept references
        assertEquals(recommendedTerm.getId(), getReferenceValue(concept, "prefLabelXl"));
        assertEquals(synonym.getId(), getReferenceValue(concept, "altLabelXl"));
        assertEquals(searchTerm.getId(), getReferenceValue(concept, "searchTerm"));

        // Term properties
        assertEquals("testitermi", getPropertyValue(recommendedTerm, "prefLabel"));
        assertEquals(Status.VALID.name(), getPropertyValue(recommendedTerm, "status"));
        assertEquals("käyttöala", getPropertyValue(recommendedTerm, "scope"));
        assertEquals("1", getPropertyValue(recommendedTerm, "termHomographNumber"));
        assertEquals("Termin lisätieto", getPropertyValue(recommendedTerm, "termInfo"));
        assertEquals("ylläpitäjän muistiinpano", getPropertyValue(recommendedTerm, "editorialNote"));
        assertEquals("broader", getPropertyValue(recommendedTerm, "termEquivalency"));
        assertEquals("wordClass", getPropertyValue(recommendedTerm, "wordClass"));
        assertEquals("feminiini", getPropertyValue(recommendedTerm, "termFamily"));
        assertEquals("monikko", getPropertyValue(recommendedTerm, "termConjugation"));

        assertEquals("Lähde 2", getPropertyValue(synonym, "source"));
        assertEquals(">", getPropertyValue(synonym, "termEquivalency"));
    }

    @Test
    public void testMapStatus() throws Exception {
        VOCABULARY vocabulary = NtrfUtil.unmarshallXmlDocument(getContent("term-and-concept-with-status.xml"));

        mapper.mapNtrfDocument("xyz", UUID.randomUUID(), vocabulary, UUID.randomUUID());

        verify(termedRequester, times(2)).exchange(
                eq("/nodes"),
                eq(HttpMethod.POST),
                any(Parameters.class),
                eq(String.class),
                deleteAndSaveArgumentCaptor.capture(),
                anyString(),
                anyString());

        var nodes = deleteAndSaveArgumentCaptor.getAllValues().get(0).getSave();

        var conceptNode = getConceptNode(nodes, "c100");
        var recommendedTerm = getTerm(conceptNode, nodes, "prefLabelXl");
        var synonym = getTerm(conceptNode, nodes, "altLabelXl");
        var notRecommendedSynonym = getTerm(conceptNode, nodes, "notRecommendedSynonym");

        assertEquals(Status.VALID.name(), getPropertyValue(conceptNode, "status"));
        assertEquals(Status.VALID.name(), getPropertyValue(recommendedTerm, "status"));
        assertEquals(Status.DRAFT.name(), getPropertyValue(synonym, "status"));
        assertEquals(Status.DRAFT.name(), getPropertyValue(notRecommendedSynonym, "status"));
    }

    @Test
    public void testMapType() throws Exception {
        VOCABULARY vocabulary = NtrfUtil.unmarshallXmlDocument(getContent("concept-type.xml"));

        mapper.mapNtrfDocument("xyz", UUID.randomUUID(), vocabulary, UUID.randomUUID());

        verify(termedRequester, times(2)).exchange(
                eq("/nodes"),
                eq(HttpMethod.POST),
                any(Parameters.class),
                eq(String.class),
                deleteAndSaveArgumentCaptor.capture(),
                anyString(),
                anyString());

        var nodes = deleteAndSaveArgumentCaptor.getAllValues().get(0).getSave();

        var c100 = getConceptNode(nodes, "c100");
        var c200 = getConceptNode(nodes, "c200");

        // if concept's type = 'aputermi', it is stored as an editorial note
        assertEquals("aputermi", getPropertyValue(c100, "editorialNote"));

        // type = vanhentunut -> status = RETIRED
        assertEquals(Status.RETIRED.name(), getPropertyValue(c200, "status"));
    }

    private GenericNode getConceptNode(List<GenericNode> nodes, String conceptId) {
        return nodes.stream()
                .filter(n -> n.getCode().equals(conceptId))
                .findFirst()
                .orElseGet(null);
    }

    private GenericNode getTerm(GenericNode concept, List<GenericNode> nodes, String reference) {
        UUID id = concept.getReferences().get(reference).get(0).getId();
        return nodes.stream()
            .filter(n -> n.getId().equals(id))
            .findFirst()
            .orElseGet(null);
    }

    private String getPropertyValue(GenericNode node, String property) {
        return node.getProperties().get(property).get(0).getValue();
    }

    private UUID getReferenceValue(GenericNode node, String property) {
        return node.getReferences().get(property).get(0).getId();
    }

    private List<String> getPropertyValues(GenericNode node, String property) {
        return node.getProperties().get(property)
                .stream()
                .map(p -> p.getValue())
                .collect(Collectors.toList());
    }

    @NotNull
    private String getContent(String fileName) throws IOException {
        InputStream data = getClass().getResourceAsStream("/" + fileName);

        String content;
        try(Reader reader = new InputStreamReader(data)) {
            content = CharStreams.toString(reader);
        }
        return content;
    }

    private void mockTermedGetGraph() {
        when(termedService.getGraph(any(UUID.class)))
                .thenReturn(new Graph(
                        UUID.randomUUID(),
                        "test",
                        "https://uri.suomi.fi/terminology/test",
                        emptyList(),
                        emptyMap(),
                        emptyMap()
                ));
    }

    private void mockTermedTypes() {

        List<MetaNode> metaNodes = new ArrayList<>();

        asList("TerminologicalVocabulary", "Concept", "Term", "Collection", "ConceptLink")
                .forEach(t -> {
                    metaNodes.add(new MetaNode(
                            t,
                            "http://uri/" + t,
                            1L,
                            new GraphId(UUID.randomUUID()),
                            emptyMap(),
                            emptyMap(),
                            emptyList(),
                            emptyList()
                    ));
                });

        when(termedService.getTypes(any(UUID.class)))
                .thenReturn(metaNodes);
    }

    private void mockUser() {
        when(userProvider.getUser())
                .thenReturn(new YtiUser(
                        "admin@localhost",
                        "Admin",
                        "Test",
                        UUID.randomUUID(),
                        false,
                        false,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        emptyMap(),
                        null,
                        null
                ));
    }
}
