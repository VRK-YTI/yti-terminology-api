package fi.vm.yti.terminology.api.importapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.util.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({
        ExcelImportJmsListener.class
})
public class ExcelImportListenerTest {

    @MockBean
    TermedRequester requester;

    @MockBean
    YtiMQService mqService;

    @Autowired
    ExcelImportJmsListener listener;

    @Captor
    ArgumentCaptor<GenericDeleteAndSave> payloadCaptor;

    private Message<List<GenericNode>> message;

    private String vocabularyId = UUID.randomUUID().toString();
    private String jobToken = UUID.randomUUID().toString();
    private String userId = UUID.randomUUID().toString();

    private UUID nodeId = UUID.randomUUID();

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        message = mock(Message.class);
        // Response for getting existing node ids
        JsonNode response = new ObjectMapper().readTree("[{\"id\": \"" + nodeId.toString()  + "\"}]");

        when(message.getPayload()).thenReturn(List.of(getNode(nodeId)));
        when(requester.exchange(
                eq("/node-trees"),
                eq(HttpMethod.GET),
                any(Parameters.class),
                eq(JsonNode.class))).thenReturn(response);
    }

    @Test
    public void testPayload() {
        UUID nonExistingNodeId = UUID.randomUUID();
        when(message.getPayload()).thenReturn(
                List.of(
                        getNode(nodeId),
                        getNode(nonExistingNodeId)
                )
        );
        listener.importNodes(message, jobToken, userId, "http://uri", 1, 2, vocabularyId);
        verify(requester)
                .exchange(anyString(),
                        any(HttpMethod.class),
                        any(Parameters.class),
                        any(Class.class),
                        payloadCaptor.capture(),
                        anyString(),
                        anyString());

        assertEquals(payloadCaptor.getValue().getDelete().size(), 0);
        assertEquals(payloadCaptor.getValue().getSave().get(0).getId(), nonExistingNodeId);
        assertEquals(payloadCaptor.getValue().getPatch().get(0).getId(), nodeId);
    }

    @Test
    public void batchProcessing() {
        listener.importNodes(message, jobToken, userId, "http://uri", 1, 2, vocabularyId);
        verify(mqService).setStatus(eq(YtiMQService.STATUS_PROCESSING), anyString(),
                anyString(), anyString(), anyString());
    }

    @Test
    public void batchReady() {
        listener.importNodes(message, jobToken, userId, "http://uri", 2, 2, vocabularyId);
        verify(mqService).setStatus(eq(YtiMQService.STATUS_READY), anyString(),
                anyString(), anyString(), anyString());
    }

    @Test
    public void batchFailed() {
        doThrow(RuntimeException.class)
                .when(requester)
                    .exchange(anyString(),
                        any(HttpMethod.class),
                        any(Parameters.class),
                        any(Class.class),
                        any(GenericDeleteAndSave.class),
                        anyString(),
                        anyString());

        listener.importNodes(message, jobToken, userId, "http://uri", 1, 2, vocabularyId);
        listener.importNodes(message, jobToken, userId, "http://uri", 2, 2, vocabularyId);

        verify(mqService).setStatus(eq(YtiMQService.STATUS_FAILED), anyString(),
                anyString(), anyString(), anyString());

        // In case of failure do not try to save other batches
        verify(requester, atMost(1)).exchange(anyString(),
                any(HttpMethod.class),
                any(Parameters.class),
                any(Class.class),
                any(GenericDeleteAndSave.class),
                anyString(),
                anyString());
    }

    private GenericNode getNode(UUID uuid) {
        return new GenericNode(uuid,
                "code", "uri", 0L,
                null, null, null, null,
                new TypeId(NodeType.Concept, new GraphId(UUID.randomUUID())),
                emptyMap(), emptyMap(), emptyMap());
    }
}
