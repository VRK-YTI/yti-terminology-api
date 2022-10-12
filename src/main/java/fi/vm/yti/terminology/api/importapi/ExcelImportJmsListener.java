package fi.vm.yti.terminology.api.importapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.GenericDeleteAndSave;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.util.Parameters;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpMethod.GET;
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

    Cache<String, Set<String>> nodeIdCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
    /**
     * Receives message from the queue and saves the batch to Termed
     */
    @JmsListener(destination = "${mq.active.subsystem}ExcelImport")
    public void importNodes(final Message<List<GenericNode>> message,
                            @Header String jobtoken,
                            @Header String userId,
                            @Header String uri,
                            @Header Integer currentBatch,
                            @Header Integer totalBatchCount,
                            @Header String vocabularyId) {

        LOGGER.info("Handling batch {}/{}, jobtoken {}, size {}. Sent by user {}", currentBatch, totalBatchCount,
                jobtoken, message.getPayload().size(), userId);

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", "true");
        params.add("append", "false");

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

            GenericDeleteAndSave payload = getPayload(vocabularyId, message.getPayload());
            StopWatch sw = StopWatch.createStarted();
            LOGGER.info("Saving to Termed, save: {}, patch: {}", payload.getSave().size(), payload.getPatch().size());

            requester.exchange("/nodes", POST, params, String.class,
                    payload, userId, "user");

            LOGGER.info("Batch {}/{} handled, jobtoken {}. Imported {} nodes in {}ms",
                    currentBatch, totalBatchCount, jobtoken, message.getPayload().size(), sw.getTime());

            response.setProcessingProgress(currentBatch);
            response.setProcessingTotal(totalBatchCount);
            response.addStatusMessage(new ImportStatusMessage(
                    ImportStatusMessage.Level.INFO,
                    "Vocabulary",
                    String.format("Saved %d nodes", message.getPayload().size()))
            );
            if (currentBatch.equals(totalBatchCount)) {
                response.setStatus(ImportStatusResponse.ImportStatus.SUCCESS);
                status = YtiMQService.STATUS_READY;
                nodeIdCache.invalidate(vocabularyId);
            } else {
                response.setStatus(ImportStatusResponse.ImportStatus.PROCESSING);
            }
        } catch (Exception e) {
            nodeIdCache.invalidate(vocabularyId);

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

    /**
     * Constructs payload for termed. If node exists add to patch list. If not, add it to save list
     */
    private GenericDeleteAndSave getPayload(String graphId, List<GenericNode> nodes) {
        Set<String> nodeIds = nodeIdCache.getIfPresent(graphId);

        if (nodeIds == null) {
            nodeIds = new HashSet<>();
            Parameters parameters = new Parameters();
            parameters.add("select", "id");
            parameters.add("where", "graph.id:" + graphId);
            parameters.add("max", "-1");

            StopWatch sw = StopWatch.createStarted();
            JsonNode existingNodes = requester.exchange("/node-trees", GET, parameters, JsonNode.class);
            for (JsonNode n : existingNodes) {
                nodeIds.add(n.get("id").textValue());
            }
            LOGGER.info("Fetch node ids in {}ms", sw.getTime());
            nodeIdCache.put(graphId, nodeIds);
        }

        List<GenericNode> save = new ArrayList<>();
        List<GenericNode> patch = new ArrayList<>();

        for (GenericNode node : nodes) {
            if (nodeIds.contains(node.getId().toString())) {
                patch.add(node);
            } else {
                save.add(node);
            }
        }

        return new GenericDeleteAndSave(Collections.emptyList(), save, patch);
    }
}
