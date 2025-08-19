package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdf.model.Model;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        ImportService.class,
})
class ImportServiceTest {

    @MockBean
    TerminologyRepository terminologyRepository;

    @MockBean
    TerminologyAuthorizationManager authorizationManager;

    @MockBean
    IndexService indexService;

    @Captor
    ArgumentCaptor<Model> modelCaptor;

    @Autowired
    ImportService service;

    ModelWrapper model;

    @BeforeEach
    void setUp() {
        var prefix = "test";
        model = TestUtils.getDefaultModel(prefix);
        when(terminologyRepository.fetchByPrefix(prefix)).thenReturn(model);
        when(authorizationManager.hasRightsToTerminology(eq(prefix), any(Model.class))).thenReturn(true);
        when(authorizationManager.getUser()).thenReturn(TestUtils.mockUser);
    }

    @Test
    void testNTRFImport() throws IOException {
        var file = getMockFile("/ntrf/ntrf-simple.xml");

        service.importNTRF(model.getPrefix(), file);

        verify(terminologyRepository).put(eq(model.getGraphURI()), modelCaptor.capture());
        verify(indexService).reindexTerminology(any(ModelWrapper.class));

        var savedModel = modelCaptor.getValue();
        var concept = savedModel.getResource(TerminologyURI.Factory.createConceptURI(model.getPrefix(), "c1").getResourceURI());
        var collection = savedModel.getResource(TerminologyURI.Factory.createConceptCollectionURI(model.getPrefix(), "collection-1").getResourceURI());

        assertTrue(concept.hasProperty(SKOS.prefLabel));
        assertTrue(collection.hasProperty(SKOS.member));
    }

    @Test
    void testExcelImport() throws IOException {
        var file = getMockFile("/excel/excel_import_simple.xlsx");

        service.importExcel(model.getPrefix(), file);

        verify(terminologyRepository).put(eq(model.getGraphURI()), modelCaptor.capture());
        verify(indexService).reindexTerminology(any(ModelWrapper.class));

        var savedModel = modelCaptor.getValue();
        var concept = savedModel.getResource(TerminologyURI.Factory.createConceptURI(model.getPrefix(), "concept-0").getResourceURI());

        assertTrue(concept.listProperties().hasNext());
    }

    private MultipartFile getMockFile(String name) throws IOException {
        var file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(getClass().getResourceAsStream(name));
        return file;
    }
}
