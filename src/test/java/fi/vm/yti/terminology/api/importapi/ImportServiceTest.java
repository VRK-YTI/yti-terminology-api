package fi.vm.yti.terminology.api.importapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.exception.NamespaceInUseException;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.NodeType;
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

import java.io.InputStream;
import java.util.*;

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
