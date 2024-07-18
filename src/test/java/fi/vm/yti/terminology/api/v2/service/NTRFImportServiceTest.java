package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        NTRFImportService.class,
})
class NTRFImportServiceTest {

    @MockBean
    TerminologyRepository terminologyRepository;

    @MockBean
    TerminologyAuthorizationManager authorizationManager;

    @MockBean
    IndexService indexService;

    @Captor
    ArgumentCaptor<Model> modelCaptor;

    @Autowired
    NTRFImportService service;

    @Test
    void testNTRFImport() throws IOException {
        var prefix = "test";
        var model = TestUtils.getDefaultModel(prefix);

        when(terminologyRepository.fetchByPrefix(prefix)).thenReturn(model);
        when(authorizationManager.hasRightsToTerminology(eq(prefix), any(Model.class))).thenReturn(true);
        when(authorizationManager.getUser()).thenReturn(TestUtils.mockUser);
        var file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(getClass().getResourceAsStream("/ntrf/ntrf-simple.xml"));

        service.importNTRF(prefix, file);

        verify(terminologyRepository).put(eq(model.getGraphURI()), modelCaptor.capture());
        verify(indexService).reindexTerminology(any(ModelWrapper.class));

        var savedModel = modelCaptor.getValue();
        var concept = savedModel.getResource(TerminologyURI.createConceptURI(prefix, "c1").getResourceURI());
        var collection = savedModel.getResource(TerminologyURI.createConceptCollectionURI(prefix, "collection-1").getResourceURI());

        assertTrue(concept.hasProperty(SKOS.prefLabel));
        assertTrue(collection.hasProperty(SKOS.member));
    }
}
