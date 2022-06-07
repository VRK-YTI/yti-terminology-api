package fi.vm.yti.terminology.api.importapi;

import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.util.Parameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.*;

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

    @Test
    public void batchProcessing() {
        Message<GenericDeleteAndSave> message = mock(Message.class);
        when(message.getPayload()).thenReturn(
                new GenericDeleteAndSave(
                        Collections.emptyList(), List.of(getNode()))
        );
        var jobToken = UUID.randomUUID().toString();
        var userId = UUID.randomUUID().toString();

        listener.importNodes(message, jobToken, userId, "http://uri", 1, 2);

        verify(mqService).setStatus(eq(YtiMQService.STATUS_PROCESSING), anyString(),
                anyString(), anyString(), anyString());
    }

    @Test
    public void batchReady() {
        Message<GenericDeleteAndSave> message = mock(Message.class);
        when(message.getPayload()).thenReturn(
                new GenericDeleteAndSave(
                        Collections.emptyList(), List.of(getNode()))
        );
        var jobToken = UUID.randomUUID().toString();
        var userId = UUID.randomUUID().toString();

        listener.importNodes(message, jobToken, userId, "http://uri", 2, 2);

        verify(mqService).setStatus(eq(YtiMQService.STATUS_READY), anyString(),
                anyString(), anyString(), anyString());
    }

    @Test
    public void batchFailed() {
        Message<GenericDeleteAndSave> message = mock(Message.class);
        when(message.getPayload()).thenReturn(
                new GenericDeleteAndSave(
                        Collections.emptyList(), List.of(getNode()))
        );
        var jobToken = UUID.randomUUID().toString();
        var userId = UUID.randomUUID().toString();

        doThrow(RuntimeException.class)
                .when(requester)
                    .exchange(anyString(),
                        any(HttpMethod.class),
                        any(Parameters.class),
                        any(Class.class),
                        any(GenericDeleteAndSave.class),
                        anyString(),
                        anyString());

        listener.importNodes(message, jobToken, userId, "http://uri", 1, 2);
        listener.importNodes(message, jobToken, userId, "http://uri", 2, 2);

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

    private GenericNode getNode() {
        return new GenericNode(
                "code", "uri", 0L,
                null, null, null, null,
                new TypeId(NodeType.Concept, new GraphId(UUID.randomUUID())),
                emptyMap(), emptyMap(), emptyMap());
    }
}
