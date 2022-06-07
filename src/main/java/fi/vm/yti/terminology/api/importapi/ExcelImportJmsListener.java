package fi.vm.yti.terminology.api.importapi;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.GenericDeleteAndSave;
import fi.vm.yti.terminology.api.util.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpMethod.POST;

@Component
public class ExcelImportJmsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelImportJmsListener.class);

    private final TermedRequester requester;

    private final YtiMQService mqService;

    public ExcelImportJmsListener(TermedRequester requester, YtiMQService mqService) {
        this.requester = requester;
        this.mqService = mqService;
    }

    // Cache to hold status information for particular job token
    Cache<String, ImportStatusResponse> statusResponseCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    /**
     * Receives message from the queue and saves the batch to Termed
     */
    @JmsListener(destination = "${mq.active.subsystem}ExcelImport")
    public void importNodes(final Message<GenericDeleteAndSave> message,
                            @Header String jobtoken,
                            @Header String userId,
                            @Header String uri,
                            @Header Integer currentBatch,
                            @Header Integer totalBatchCount) {

        LOGGER.info("Handling batch {}/{}, jobtoken {}. Sent by user {}", currentBatch, totalBatchCount, jobtoken, userId);

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", "true");

        ImportStatusResponse response = statusResponseCache.getIfPresent(jobtoken);

        if (response == null) {
            response = new ImportStatusResponse();
        }

        int status = YtiMQService.STATUS_PROCESSING;

        try {

            if (response.getStatus() == ImportStatusResponse.ImportStatus.FAILURE) {
                // Previous batch failed. Do not handle rest of batches
                LOGGER.info("Skip batch due to previous error, {}", jobtoken);
                return;
            }

            requester.exchange("/nodes", POST, params, String.class, message.getPayload(), userId, "user");

            LOGGER.info("Batch {}/{} handled, jobtoken {}. Imported {} nodes",
                    currentBatch, totalBatchCount, jobtoken, message.getPayload().getSave().size());

            response.setProcessingProgress(currentBatch);
            response.setProcessingTotal(totalBatchCount);
            response.addStatusMessage(new ImportStatusMessage(
                    ImportStatusMessage.Level.INFO,
                    "Vocabulary",
                    String.format("Saved %d nodes", message.getPayload().getSave().size()))
            );
            if (currentBatch.equals(totalBatchCount)) {
                response.setStatus(ImportStatusResponse.ImportStatus.SUCCESS);
                status = YtiMQService.STATUS_READY;
            } else {
                response.setStatus(ImportStatusResponse.ImportStatus.PROCESSING);
            }
        } catch (Exception e) {
            response.setStatus(ImportStatusResponse.ImportStatus.FAILURE);
            response.setResultsError(response.getResultsError() == null ? 1 : response.getResultsError() + 1);
            response.addStatusMessage(new ImportStatusMessage(
                            ImportStatusMessage.Level.ERROR,
                            "Vocabulary",
                            String.format("Termed error, reference: %s", jobtoken)));
            status = YtiMQService.STATUS_FAILED;

            LOGGER.error(String.format("Error saving nodes: jobtoken %s, message: %s", jobtoken, e.getMessage()), e);
        }
        statusResponseCache.put(jobtoken, response);
        mqService.setStatus(status, jobtoken, userId, uri, response.toString());
    }
}
