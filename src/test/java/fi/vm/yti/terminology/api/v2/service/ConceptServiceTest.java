package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.exception.ResourceExistsException;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.AuthorizationException;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.TermDTO;
import fi.vm.yti.terminology.api.v2.enums.ReferenceType;
import fi.vm.yti.terminology.api.v2.opensearch.IndexConcept;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.SKOSXL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        ConceptService.class,
})
class ConceptServiceTest {

    @MockBean
    TerminologyRepository repository;

    @MockBean
    TerminologyAuthorizationManager authorizationManager;

    @MockBean
    IndexService indexService;

    @MockBean
    GroupManagementService groupManagementService;

    @Captor
    ArgumentCaptor<Model> modelCaptor;

    @Captor
    ArgumentCaptor<IndexConcept> indexCaptor;

    @Autowired
    ConceptService conceptService;

    @BeforeEach
    void setUp() {
        when(authorizationManager.getUser()).thenReturn(TestUtils.mockUser);
        when(authorizationManager.hasRightsToTerminology(eq("test"), any(ModelWrapper.class))).thenReturn(true);
    }

    @Test
    void testGetConcept() {
        var conceptURI = TerminologyURI.createConceptURI("test", "concept-1");
        var model = TestUtils.getModelFromFile("/terminology-with-concepts.ttl", conceptURI.getGraphURI());

        var extRefResult = ModelFactory.createDefaultModel();
        extRefResult.createResource(TerminologyURI.createConceptURI("ext", "concept-1").getResourceURI())
                .addProperty(SKOSXL.literalForm, ResourceFactory.createLangLiteral("Ext concept label", "fi"));

        when(authorizationManager.hasRightsToTerminology(eq(conceptURI.getPrefix()), any(Model.class))).thenReturn(true);
        when(repository.fetchByPrefix(conceptURI.getPrefix())).thenReturn(model);
        when(groupManagementService.mapUser()).thenReturn(TestUtils.mapUser);
        when(repository.queryConstruct(any(Query.class))).thenReturn(extRefResult);

        var dto = conceptService.get(conceptURI.getPrefix(), conceptURI.getResourceId());

        // only authenticated user should see this information
        assertFalse(dto.getEditorialNotes().isEmpty());
        assertNotNull(dto.getCreator().getName());
        assertNotNull(dto.getModifier().getName());

        var extRef = dto.getReferences().stream().filter(r -> r.getReferenceType().equals(ReferenceType.NARROW_MATCH)).findFirst();

        assertTrue(extRef.isPresent());
        assertEquals(Map.of("fi", "Ext concept label"), extRef.get().getLabel());
    }

    @Test
    void testGetConceptNotAuthenticated() {
        var conceptURI = TerminologyURI.createConceptURI("test", "concept-1");
        var model = TestUtils.getModelFromFile("/terminology-with-concepts.ttl", conceptURI.getGraphURI());

        when(authorizationManager.hasRightsToTerminology(eq(conceptURI.getPrefix()), any(Model.class))).thenReturn(false);
        when(repository.fetchByPrefix(conceptURI.getPrefix())).thenReturn(model);
        when(groupManagementService.mapUser()).thenReturn(TestUtils.mapUser);
        when(repository.queryConstruct(any(Query.class))).thenReturn(ModelFactory.createDefaultModel());

        var dto = conceptService.get(conceptURI.getPrefix(), conceptURI.getResourceId());

        // only authenticated user should see this information
        assertTrue(dto.getEditorialNotes().isEmpty());
        assertNull(dto.getCreator().getName());
        assertNull(dto.getModifier().getName());
    }

    @Test
    void testGetConceptNotFound() {
        var conceptURI = TerminologyURI.createConceptURI("test", "concept-1");
        var model = TestUtils.getModelFromFile("/terminology-with-concepts.ttl", conceptURI.getGraphURI());
        when(repository.fetchByPrefix(conceptURI.getPrefix())).thenReturn(model);

        assertThrows(ResourceNotFoundException.class, () -> conceptService.get("test", "foo"));
    }

    @Test
    void testCreateConcept() throws URISyntaxException {
        var conceptURI = TerminologyURI.createConceptURI("test", "concept-1");
        var model = new ModelWrapper(ModelFactory.createDefaultModel(), conceptURI.getGraphURI());

        when(repository.fetchByPrefix(conceptURI.getPrefix())).thenReturn(model);
        when(repository.resourceExistsInGraph(conceptURI.getGraphURI(), conceptURI.getResourceURI())).thenReturn(false);

        var dto = new ConceptDTO();
        dto.setIdentifier(conceptURI.getResourceId());
        dto.setStatus(Status.DRAFT);

        var term = new TermDTO();
        term.setLanguage("en");
        term.setLabel("test");
        dto.setRecommendedTerms(List.of(term));

        conceptService.create(conceptURI.getPrefix(), dto);

        verify(repository).put(eq(conceptURI.getGraphURI()), modelCaptor.capture());
        verify(indexService).addConceptToIndex(indexCaptor.capture());

        var updatedModel = modelCaptor.getValue();

        assertTrue(updatedModel.getResource(conceptURI.getResourceURI()).listProperties().hasNext());
        assertEquals(conceptURI.getResourceURI(), indexCaptor.getValue().getId());
    }

    @Test
    void testCreateConceptExists() {
        var conceptURI = TerminologyURI.createConceptURI("test", "concept-1");
        var model = new ModelWrapper(ModelFactory.createDefaultModel(), conceptURI.getGraphURI());

        when(repository.fetchByPrefix(conceptURI.getPrefix())).thenReturn(model);
        when(repository.resourceExistsInGraph(conceptURI.getGraphURI(), conceptURI.getResourceURI())).thenReturn(true);

        var dto = new ConceptDTO();
        dto.setIdentifier(conceptURI.getResourceId());

        assertThrows(ResourceExistsException.class, () -> conceptService.create("test", dto));

        verify(repository, times(0)).put(eq("test"), any(Model.class));
        verifyNoMoreInteractions(indexService);
    }

    @Test
    void testCreateConceptGraphNotExists() {
        var prefix = "test123";
        var conceptURI = TerminologyURI.createConceptURI(prefix, "concept-1");

        when(repository.fetchByPrefix(prefix)).thenThrow(ResourceNotFoundException.class);
        when(repository.resourceExistsInGraph(conceptURI.getGraphURI(), conceptURI.getResourceURI())).thenReturn(false);

        var dto = new ConceptDTO();
        dto.setIdentifier(conceptURI.getResourceId());

        assertThrows(ResourceNotFoundException.class, () -> conceptService.create(prefix, dto));

        verify(repository, times(0)).put(eq(prefix), any(Model.class));
        verifyNoMoreInteractions(indexService);
    }

    @Test
    void testCreateConceptNotAuthorized() {
        when(authorizationManager.hasRightsToTerminology(eq("test"), any(Model.class))).thenReturn(false);

        var dto = new ConceptDTO();
        dto.setIdentifier("concept-1");

        assertThrows(AuthorizationException.class, () -> conceptService.create("test", dto));

        verify(repository, times(0)).put(eq("test"), any(Model.class));
    }

    @Test
    void testUpdateConcept() {
        var conceptURI = TerminologyURI.createConceptURI("test", "concept-1");
        var model = new ModelWrapper(ModelFactory.createDefaultModel(), conceptURI.getGraphURI());

        model.createResourceWithId(conceptURI.getResourceId())
                        .addProperty(SuomiMeta.publicationStatus, MapperUtils.getStatusUri(Status.DRAFT));

        when(repository.fetchByPrefix(conceptURI.getPrefix())).thenReturn(model);
        when(repository.resourceExistsInGraph(conceptURI.getGraphURI(), conceptURI.getResourceURI())).thenReturn(true);

        var dto = new ConceptDTO();
        dto.setIdentifier(conceptURI.getResourceId());
        dto.setStatus(Status.VALID);

        var term = new TermDTO();
        term.setLanguage("en");
        term.setLabel("test");
        dto.setRecommendedTerms(List.of(term));

        conceptService.update(conceptURI.getPrefix(), conceptURI.getResourceId(), dto);

        verify(repository).put(eq(conceptURI.getGraphURI()), modelCaptor.capture());
        verify(indexService).updateConceptToIndex(indexCaptor.capture());

        var updatedResource = modelCaptor.getValue().getResource(conceptURI.getResourceURI());

        assertEquals(Status.VALID, MapperUtils.getStatus(updatedResource));
        assertEquals(conceptURI.getResourceURI(), indexCaptor.getValue().getId());
    }

    @Test
    void testUpdateConceptNotExists() {
        var prefix = "test";
        var conceptURI = TerminologyURI.createConceptURI(prefix, "concept-1");
        var model = new ModelWrapper(ModelFactory.createDefaultModel(), conceptURI.getGraphURI());

        when(repository.fetchByPrefix(conceptURI.getPrefix())).thenReturn(model);
        when(repository.resourceExistsInGraph(conceptURI.getGraphURI(), conceptURI.getResourceURI())).thenReturn(false);

        var dto = new ConceptDTO();
        dto.setIdentifier(conceptURI.getResourceId());

        assertThrows(ResourceNotFoundException.class, () -> conceptService.update(prefix, "concept-1", dto));

        verify(repository, times(0)).put(eq(prefix), any(Model.class));
        verifyNoMoreInteractions(indexService);
    }

    @Test
    void testUpdateConceptNotAuthorized() {
        when(authorizationManager.hasRightsToTerminology(eq("test"), any(Model.class))).thenReturn(false);

        var dto = new ConceptDTO();
        dto.setIdentifier("concept-1");

        assertThrows(AuthorizationException.class, () -> conceptService.update("test", "concept-1", dto));
        verify(repository, times(0)).put(eq("test"), any(Model.class));
    }

    @Test
    void testDeleteConcept() {
        var conceptURI = TerminologyURI.createConceptURI("test", "concept-2");
        var model = TestUtils.getModelFromFile("/terminology-with-concepts.ttl", conceptURI.getGraphURI());

        when(repository.fetchByPrefix(conceptURI.getPrefix())).thenReturn(model);
        when(repository.resourceExistsInGraph(conceptURI.getGraphURI(), conceptURI.getResourceURI())).thenReturn(true);

        conceptService.delete("test", "concept-2");

        // resource concept-2 should not exists and it should not exists as an object either
        assertFalse(model.listStatements(null, null, ResourceFactory.createResource(conceptURI.getResourceURI())).hasNext());
        assertFalse(model.contains(ResourceFactory.createResource(conceptURI.getResourceURI()), null));
        assertFalse(model.contains(ResourceFactory.createResource(conceptURI.getGraphURI() + "term-4429d6a0-1aba-4964-a2c0-972a13409b2f"), null));
    }

    @Test
    void testDeleteConceptNotAuthorized() {
        when(authorizationManager.hasRightsToTerminology(eq("test"), any(Model.class))).thenReturn(false);
        assertThrows(AuthorizationException.class, () -> conceptService.delete("test", "concept-1"));

        verify(repository, times(0)).put(eq("test"), any(Model.class));
    }
}
