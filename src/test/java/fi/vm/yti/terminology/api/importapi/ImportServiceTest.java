package fi.vm.yti.terminology.api.importapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.exception.ExcelParseException;
import fi.vm.yti.terminology.api.exception.NamespaceInUseException;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({
        ImportService.class
})
@TestPropertySource(properties = {
        "mq.batch.size=1"
})
public class ImportServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(ImportServiceTest.class);

    @MockBean
    FrontendGroupManagementService groupManagementService;
    @MockBean
    FrontendTermedService termedService;
    @MockBean
    AuthenticatedUserProvider userProvider;
    @MockBean
    AuthorizationManager authorizationManager;
    @MockBean
    YtiMQService ytiMQService;

    @Autowired
    ImportService importService;

    @Captor
    ArgumentCaptor<List<List<GenericNode>>> batchesCaptor;

    @Captor
    ArgumentCaptor<GenericNode> nodeCaptor;

    @Captor
    ArgumentCaptor<UUID> uuidCaptor;

    @Captor
    ArgumentCaptor<String> stringCaptor;

    UUID organizationId = UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63");

    private static final String TEMPLATE_GRAPH_ID = "3aa764fc-6b32-4a87-b64e-887caab128b1";


    public void mockCommon() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode groups = null;

        try {
            InputStream is = this.getClass().getResourceAsStream("/groups.json");
            groups = mapper.readTree(is);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        when(termedService.getNodeListWithoutReferencesOrReferrers(
                eq(NodeType.Group)
        )).thenReturn(groups);

        Map<UUID, Set<Role>> rolesInOrganizations = new HashMap<>();
        rolesInOrganizations.put(organizationId, Set.of(Role.TERMINOLOGY_EDITOR));

        when(userProvider.getUser()).thenReturn(new YtiUser(
                "admin@localhost",
                "Admin",
                "Test",
                UUID.randomUUID(),
                true,
                false,
                null,
                null,
                rolesInOrganizations,
                null,
                null
        ));

        when(authorizationManager.canCreateVocabulary(any(GenericNode.class))).thenReturn(true);
    }

    @Test
    public void handleImportTerminologyExists() throws Exception {
        mockCommon();
        InputStream is = this.getClass().getResourceAsStream("/importapi/excel/excel_export.xlsx");

        importService.handleExcelImport(is);

        verify(ytiMQService).handleExcelImportAsync(
                any(UUID.class),
                any(MessageHeaderAccessor.class),
                stringCaptor.capture(),
                batchesCaptor.capture());

        assertEquals("http://uri.suomi.fi/terminology/testdev/terminological-vocabulary-0", stringCaptor.getValue());
        assertEquals(2, batchesCaptor.getValue().size());

        // terminology, concepts, concept links and terms
        assertEquals(7, batchesCaptor.getValue().get(0).size());

        // collections in separate batch
        assertEquals(1, batchesCaptor.getValue().get(1).size());
    }

    @Test
    public void handleImportNewTerminology() throws Exception {
        mockCommon();
        InputStream is = this.getClass().getResourceAsStream("/importapi/excel/excel_export.xlsx");

        doThrow(NullPointerException.class)
                .when(termedService)
                .getGraph(any(UUID.class));

        importService.handleExcelImport(is);

        verify(termedService).createVocabulary(
                any(UUID.class),
                stringCaptor.capture(),
                nodeCaptor.capture(),
                uuidCaptor.capture(),
                anyBoolean());

        assertEquals("testdev", stringCaptor.getValue());
        assertEquals("3aa764fc-6b32-4a87-b64e-887caab128b1", uuidCaptor.getValue().toString());
        assertEquals("Test terminology fi", nodeCaptor.getValue().getProperties().get("prefLabel").get(0).getValue());
    }

    @Test
    public void handleImportSimpleExcel() throws IOException {
        mockCommon();
        InputStream is = this.getClass().getResourceAsStream("/importapi/excel/simple_import.xlsx");

        var labelProperties = Map.of("prefLabel",
                List.of(new Property("fi", "finlabel")
                        , new Property("sv", "swelabel")));

        var defaultGraph = new Graph(UUID.fromString(TEMPLATE_GRAPH_ID), "test", "http://uri.suomi.fi/terminology/test", emptyList(), emptyMap(), labelProperties);

        when(termedService.getGraph(any())).thenReturn(defaultGraph);
        when(authorizationManager.canModifyNodes(anyList())).thenReturn(true);

        importService.handleSimpleExcelImport(UUID.fromString(TEMPLATE_GRAPH_ID), is);

        verify(ytiMQService).handleExcelImportAsync(
                any(UUID.class),
                any(MessageHeaderAccessor.class),
                stringCaptor.capture(),
                batchesCaptor.capture());

        assertEquals("http://uri.suomi.fi/terminology/test", stringCaptor.getValue());
        assertEquals(1, batchesCaptor.getValue().size());

        // 1 concept and 4 terms
        // 2 prefLabel different language
        // 2 altLabel same language
        assertEquals(5, batchesCaptor.getValue().get(0).size());
    }

    @Test
    public void handleImportSimpleExcelMissingLanguage() {
        mockCommon();
        InputStream is = this.getClass().getResourceAsStream("/importapi/excel/simple_import.xlsx");

        var labelProperties = Map.of("prefLabel",
                List.of(new Property("sv", "swelabel")));

        var defaultGraph = new Graph(UUID.fromString(TEMPLATE_GRAPH_ID), "test", "http://uri.suomi.fi/terminology/test", emptyList(), emptyMap(), labelProperties);

        when(termedService.getGraph(any())).thenReturn(defaultGraph);

        ExcelParseException exception = assertThrows(ExcelParseException.class, () -> importService.handleSimpleExcelImport(UUID.fromString(TEMPLATE_GRAPH_ID), is));
        assertTrue(exception.getMessage().contains("Language does not exist in terminology"));
    }

    @Test
    public void handleImportSimpleExcelNoTerminology() {
        mockCommon();
        InputStream is = this.getClass().getResourceAsStream("/importapi/excel/simple_import.xlsx");

        doThrow(NullPointerException.class)
                .when(termedService)
                .getGraph(any(UUID.class));

        NullPointerException exception = assertThrows(NullPointerException.class, () -> importService.handleSimpleExcelImport(UUID.fromString(TEMPLATE_GRAPH_ID), is));
        assertEquals("Terminology doesnt exist", exception.getMessage());
    }

    @Test
    public void handleImportSimpleExcelMissingTermLanguage() {
        mockCommon();
        InputStream is = this.getClass().getResourceAsStream("/importapi/excel/simple_import_term_missing_lang.xlsx");

        var labelProperties = Map.of("prefLabel",
                List.of(new Property("fi", "finlabel")
                        , new Property("sv", "swelabel")));

        var defaultGraph = new Graph(UUID.fromString(TEMPLATE_GRAPH_ID), "test", "http://uri.suomi.fi/terminology/test", emptyList(), emptyMap(), labelProperties);

        when(termedService.getGraph(any())).thenReturn(defaultGraph);

        ExcelParseException exception = assertThrows(ExcelParseException.class, () -> importService.handleSimpleExcelImport(UUID.fromString(TEMPLATE_GRAPH_ID), is));
        assertTrue(exception.getMessage().contains("Term name missing language suffix"));
    }

    @Test
    public void namespaceAlreadyInUse() {
        mockCommon();
        InputStream is = this.getClass().getResourceAsStream("/importapi/excel/excel_export.xlsx");

        doThrow(NullPointerException.class)
                .when(termedService)
                .getGraph(any(UUID.class));

        when(termedService.isNamespaceInUse(anyString())).thenReturn(true);

        assertThrows(NamespaceInUseException.class, () -> {
            importService.handleExcelImport(is);
        });

        verifyNoInteractions(ytiMQService);
    }
}
