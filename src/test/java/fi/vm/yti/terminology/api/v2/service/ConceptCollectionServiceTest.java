package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.exception.ResourceExistsException;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.AuthorizationException;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptReferenceInfoDTO;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;

import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        ConceptCollectionService.class,
})
class ConceptCollectionServiceTest {
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

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    ConceptCollectionService conceptCollectionService;

    @BeforeEach
    void setUp() {
        when(authorizationManager.getUser()).thenReturn(TestUtils.mockUser);
        when(authorizationManager.hasRightsToTerminology(eq("test"), any(ModelWrapper.class))).thenReturn(true);
    }

    @Test
    void testGetConceptCollection() {
        var conceptCollectionURI = TerminologyURI.Factory.createConceptCollectionURI(
                "test",
                "collection-1");
        var model = TestUtils.getModelFromFile(
                "/terminology-with-concept-collections.ttl",
                conceptCollectionURI.getGraphURI());

        when(authorizationManager.hasRightsToTerminology(eq(conceptCollectionURI.getPrefix()), any(Model.class))).thenReturn(true);
        when(repository.fetchByPrefix(conceptCollectionURI.getPrefix())).thenReturn(model);
        when(groupManagementService.mapUser()).thenReturn(TestUtils.mapUser);

        var dto = conceptCollectionService.get(
                conceptCollectionURI.getPrefix(),
                conceptCollectionURI.getResourceId());

        assertFalse(dto.getLabel().isEmpty());
        assertFalse(dto.getDescription().isEmpty());

        // only authenticated user should see this information
        assertNotNull(dto.getCreator().getName());
        assertNotNull(dto.getModifier().getName());
    }

    @Test
    void testGetConceptCollectionNotAuthenticated() {
        var conceptCollectionURI = TerminologyURI.Factory.createConceptCollectionURI(
                "test",
                "collection-1");
        var model = TestUtils.getModelFromFile(
                "/terminology-with-concept-collections.ttl",
                conceptCollectionURI.getGraphURI());

        when(authorizationManager.hasRightsToTerminology(
                eq(conceptCollectionURI.getPrefix()),
                any(Model.class)))
                .thenReturn(false);
        when(repository.fetchByPrefix(conceptCollectionURI.getPrefix())).thenReturn(model);
        when(groupManagementService.mapUser()).thenReturn(TestUtils.mapUser);

        var dto = conceptCollectionService.get(
                conceptCollectionURI.getPrefix(),
                conceptCollectionURI.getResourceId());

        assertFalse(dto.getLabel().isEmpty());
        assertFalse(dto.getDescription().isEmpty());

        var members = dto.getMembers().stream()
                .map(ConceptReferenceInfoDTO::getIdentifier)
                .toList();
        assertEquals(List.of("concept-1", "concept-2"), members);

        // only authenticated user should see this information
        assertNull(dto.getCreator().getName());
        assertNull(dto.getModifier().getName());
    }

    @Test
    void testGetConceptCollectionNotFound() {
        var conceptCollectionURI = TerminologyURI.Factory.createConceptCollectionURI(
                "test",
                "collection-1");
        var model = TestUtils.getModelFromFile(
                "/terminology-with-concept-collections.ttl",
                conceptCollectionURI.getGraphURI());
        when(repository.fetchByPrefix(conceptCollectionURI.getPrefix()))
                .thenReturn(model);

        assertThrows(
                ResourceNotFoundException.class,
                () -> conceptCollectionService.get(
                        "test",
                        "foo"));
    }

    @Test
    void testCreateConceptCollection() throws URISyntaxException {
        var conceptCollectionURI = TerminologyURI.Factory.createConceptCollectionURI(
                "test",
                "collection-1");
        var model = new ModelWrapper(
                ModelFactory.createDefaultModel(),
                conceptCollectionURI.getGraphURI());
        when(repository.fetchByPrefix(conceptCollectionURI.getPrefix())).thenReturn(model);
        when(repository.resourceExistsInGraph(
                conceptCollectionURI.getGraphURI(),
                conceptCollectionURI.getResourceURI()))
                .thenReturn(false);

        var dto = new ConceptCollectionDTO();
        dto.setIdentifier(conceptCollectionURI.getResourceId());

        dto.addMember("concept-1");
        dto.addMember("concept-2");

        conceptCollectionService.create(conceptCollectionURI.getPrefix(), dto);

        verify(repository).put(
                eq(conceptCollectionURI.getGraphURI()),
                modelCaptor.capture());

        var updatedModel = modelCaptor.getValue();

        assertTrue(updatedModel.getResource(
                conceptCollectionURI.getResourceURI())
                .listProperties().hasNext());
    }

    @Test
    void testCreateConceptCollectionExists() {
        var conceptCollectionURI = TerminologyURI.Factory.createConceptCollectionURI(
                "test",
                "collection-1");
        var model = new ModelWrapper(
                ModelFactory.createDefaultModel(),
                conceptCollectionURI.getGraphURI());
        when(repository.fetchByPrefix(
                conceptCollectionURI.getPrefix()))
                .thenReturn(model);
        when(repository.resourceExistsInGraph(
                conceptCollectionURI.getGraphURI(),
                conceptCollectionURI.getResourceURI()))
                .thenReturn(true);

        var dto = new ConceptCollectionDTO();
        dto.setIdentifier(conceptCollectionURI.getResourceId());

        assertThrows(
                ResourceExistsException.class,
                () -> conceptCollectionService.create("test", dto));

        verify(repository,
                times(0)).put(eq("test"),
                any(Model.class));
        verifyNoMoreInteractions(indexService);
    }

    @Test
    void testCreateConceptCollectionGraphNotExists() {
        var prefix = "test456";
        var conceptCollectionURI = TerminologyURI.Factory.createConceptCollectionURI(
                prefix,
                "collection-1");
        when(repository.fetchByPrefix(prefix)).thenThrow(ResourceNotFoundException.class);
        when(repository.resourceExistsInGraph(
                conceptCollectionURI.getGraphURI(),
                conceptCollectionURI.getResourceURI())).thenReturn(false);

        var dto = new ConceptCollectionDTO();
        dto.setIdentifier(conceptCollectionURI.getResourceId());

        assertThrows(
                ResourceNotFoundException.class,
                () -> conceptCollectionService.create(prefix, dto));

        verify(repository,
                times(0)).put(eq(prefix),
                any(Model.class));
        verifyNoMoreInteractions(indexService);
    }

    @Test
    void testCreateConceptCollectionNotAuthorized() {
        when(authorizationManager.hasRightsToTerminology(
                eq("test"),
                any(Model.class)))
                .thenReturn(false);

        var dto = new ConceptCollectionDTO();
        dto.setIdentifier("collection-1");

        assertThrows(
                AuthorizationException.class,
                () -> conceptCollectionService.create("test", dto));

        verify(repository, times(0)).put(
                eq("test"),
                any(Model.class));
    }

    @Test
    void testUpdateConceptCollection() {
        var conceptCollectionURI = TerminologyURI.Factory.createConceptCollectionURI(
                "test",
                "collection-1");
        var model = new ModelWrapper(
                ModelFactory.createDefaultModel(),
                conceptCollectionURI.getGraphURI());

        model.createResourceWithId(conceptCollectionURI.getResourceId())
                .addProperty(
                        SKOS.member,
                        model.createList(
                                Stream.of("concept-1", "concept-2")
                                        .map(ResourceFactory::createStringLiteral)
                                        .iterator()));

        when(repository.fetchByPrefix(conceptCollectionURI.getPrefix()))
                .thenReturn(model);
        when(repository.resourceExistsInGraph(
                conceptCollectionURI.getGraphURI(),
                conceptCollectionURI.getResourceURI()))
                .thenReturn(true);

        var dto = new ConceptCollectionDTO();
        dto.setIdentifier(conceptCollectionURI.getResourceId());
        dto.addMember("concept-3");
        dto.addMember("concept-4");

        conceptCollectionService.update(
                conceptCollectionURI.getPrefix(),
                conceptCollectionURI.getResourceId(),
                dto);

        verify(repository).put(eq(conceptCollectionURI.getGraphURI()), modelCaptor.capture());

        var updatedResource = modelCaptor.getValue().getResource(conceptCollectionURI.getResourceURI());
    }
}
