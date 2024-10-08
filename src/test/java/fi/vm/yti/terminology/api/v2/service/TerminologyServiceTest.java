package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.service.FrontendService;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.security.AuthorizationException;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.opensearch.IndexTerminology;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DCTerms;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        TerminologyService.class,
})
class TerminologyServiceTest {

    @MockBean
    private TerminologyRepository terminologyRepository;

    @MockBean
    private TerminologyAuthorizationManager authorizationManager;

    @MockBean
    private IndexService indexService;

    @MockBean
    private FrontendService frontendService;

    @MockBean
    private GroupManagementService groupManagementService;
    
    @Autowired
    TerminologyService terminologyService;

    @Captor
    ArgumentCaptor<Model> modelCaptor;

    @Captor
    ArgumentCaptor<IndexTerminology> indexCaptor;

    private static final String GRAPH_URI = "https://iri.suomi.fi/terminology/test/";

    private static final String PREFIX = "test";

    @BeforeEach
    void setUp() {
        when(frontendService.getServiceCategories()).thenReturn(TestUtils.categoryDTOs);
        when(frontendService.getOrganizations(anyString(), anyBoolean())).thenReturn(TestUtils.organizationDTOs);
        when(authorizationManager.getUser()).thenReturn(TestUtils.mockUser);
        when(authorizationManager.hasRightToAnyOrganization(argThat(a -> a.contains(TestUtils.organizationId))))
                .thenReturn(true);
        when(authorizationManager.hasRightsToTerminology(eq(PREFIX), any(Model.class))).thenReturn(true);
        when(groupManagementService.mapUser()).thenReturn(TestUtils.mapUser);

        var model = TestUtils.getModelFromFile("/terminology-metadata.ttl", GRAPH_URI);
        when(terminologyRepository.fetchByPrefix(PREFIX)).thenReturn(model);
        when(terminologyRepository.queryConstruct(any(Query.class))).thenReturn(model);
    }

    @Test
    void testGetData() {
        var dto = terminologyService.get(PREFIX);

        assertNotNull(dto.getCreator().getName());
        assertNotNull(dto.getModifier().getName());

        var cat = dto.getGroups().iterator().next();
        var org = dto.getOrganizations().iterator().next();

        assertEquals("P10", cat.getIdentifier());
        assertEquals("Sample category P10", cat.getLabel().get("en"));
        assertEquals("Test organization", org.getLabel().get("en"));
        assertEquals(TestUtils.organizationId.toString(), org.getId());
    }

    @Test
    void testGetDataUnauthorized() {
        when(authorizationManager.hasRightsToTerminology(eq(PREFIX), any(Model.class))).thenReturn(false);
        var dto = terminologyService.get(PREFIX);

        assertNull(dto.getCreator().getName());
        assertNull(dto.getModifier().getName());
    }

    @Test
    void testCreate() throws URISyntaxException {
        terminologyService.create(getValidTerminologyMetadata());

        verify(terminologyRepository).put(eq(GRAPH_URI), modelCaptor.capture());
        verify(indexService).addTerminologyToIndex(indexCaptor.capture());

        var modelValueResource = modelCaptor.getValue().getResource(GRAPH_URI);
        var indexValue = indexCaptor.getValue();

        assertTrue(modelValueResource.listProperties().hasNext());
        assertEquals("http://urn.fi/URN:NBN:fi:au:ptvl:v1096",
                MapperUtils.propertyToString(modelValueResource, DCTerms.isPartOf));
        assertEquals(GRAPH_URI, indexValue.getId());
    }

    @Test
    void testCreateAuthorization() {
        var dto = getValidTerminologyMetadata();
        dto.setOrganizations(Set.of(UUID.randomUUID()));

        assertThrows(AuthorizationException.class, () -> terminologyService.create(dto));

        verifyNoMoreInteractions(terminologyRepository);
        verifyNoMoreInteractions(indexService);
    }

    @Test
    void testUpdate() {
        var dto = getValidTerminologyMetadata();

        terminologyService.update(PREFIX, dto);

        verify(terminologyRepository).put(eq(GRAPH_URI), modelCaptor.capture());
        verify(indexService).updateTerminologyToIndex(indexCaptor.capture());

        var modelValue = modelCaptor.getValue();
        var indexValue = indexCaptor.getValue();

        assertTrue(modelValue.getResource(GRAPH_URI).listProperties().hasNext());
        assertEquals(GRAPH_URI, indexValue.getId());
    }

    @Test
    void testUpdateAuthorization() {
        var dto = getValidTerminologyMetadata();

        when(authorizationManager.hasRightsToTerminology(eq(PREFIX), any(Model.class))).thenReturn(false);

        assertThrows(AuthorizationException.class, () -> terminologyService.update(PREFIX, dto));

        verify(terminologyRepository, times(0)).put(anyString(), any(Model.class));
        verifyNoMoreInteractions(indexService);
    }

    @Test
    void testDelete() {
        terminologyService.delete(PREFIX);

        verify(terminologyRepository).delete(GRAPH_URI);
        verify(indexService).deleteTerminologyFromIndex(GRAPH_URI);
    }

    @Test
    void testDeleteAuthorization() {
        when(authorizationManager.hasRightsToTerminology(eq(PREFIX), any(Model.class))).thenReturn(false);

        assertThrows(AuthorizationException.class, () -> terminologyService.delete(PREFIX));

        verify(terminologyRepository, times(0)).put(eq(GRAPH_URI), any(Model.class));
        verifyNoMoreInteractions(indexService);
    }

    private static TerminologyDTO getValidTerminologyMetadata() {
        var terminologyDTO = new TerminologyDTO();

        terminologyDTO.setOrganizations(Set.of(TestUtils.organizationId));
        terminologyDTO.setGraphType(GraphType.TERMINOLOGICAL_VOCABULARY);
        terminologyDTO.setContact("Contact");
        terminologyDTO.setGroups(Set.of("P10"));
        terminologyDTO.setLabel(Map.of("en", "Test terminology"));
        terminologyDTO.setDescription(Map.of("en", "Test description"));
        terminologyDTO.setLanguages(Set.of("en"));
        terminologyDTO.setPrefix(PREFIX);
        terminologyDTO.setStatus(Status.VALID);

        return terminologyDTO;
    }
}
