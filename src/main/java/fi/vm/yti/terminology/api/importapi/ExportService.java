package fi.vm.yti.terminology.api.importapi;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import fi.vm.yti.terminology.api.importapi.excel.ExcelCreator;
import fi.vm.yti.terminology.api.importapi.excel.JSONWrapper;
import jakarta.ws.rs.InternalServerErrorException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.stereotype.Service;

import fi.vm.yti.terminology.api.TermedContentType;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.util.JsonUtils;
import fi.vm.yti.terminology.api.util.Parameters;

@Service
@EnableJms
public class ExportService {

    private final TermedRequester termedRequester;
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    @Autowired
    public ExportService(TermedRequester termedRequester) {
        this.termedRequester = termedRequester;
    }

    private Parameters constructFullVocabularyIdQuery(UUID id) {
        Parameters params = new Parameters();
        params.add("select", "*, references.prefLabelXl:1");
        // Get all nodes from given graph
        // params.add("where", "graph.id:" + id + " AND (type.id:" + Vocabulary + " OR
        // type.id:"
        // + TerminologicalVocabulary + ")");
        params.add("max", "-1");
        return params;
    }

    private Parameters constructVocabularyTypeQuery(String nodeType) {
        Parameters params = new Parameters();
        params.add("select", "*,references.prefLabelXl:1");
        // Get all nodes from given graph
        if (nodeType.contains(",")) {
            params.add("where", "(type.id:"+ nodeType.replaceAll(",", " OR type.id:")+")");
        } else {
            params.add("where", "type.id:" + nodeType);
        }
        params.add("max", "-1");
        return params;
    }

    @NotNull
    JsonNode getFullVocabulary(UUID id) {
        /*
         * https://sanastot.dev.yti.cloud.vrk.fi/termed-api/graphs/5b3eb5d7-0239-484d-8515-
         * bc4b8cb42e7e/node-trees?select=*,references.prefLabelXl:1&where=type.id:
         * Concept%20OR%20type.id:Collection&max=-1
         * 
         */
        Parameters params = constructFullVocabularyIdQuery(id);
        String path = "/graphs/" + id.toString() + "/node-trees";
        // Execute full search
        JsonNode rv = requireNonNull(termedRequester.exchange(path, GET, params, JsonNode.class));
        return requireNonNull(rv);
    }

    @NotNull
    JsonNode getVocabulary(UUID id, String nodeTypes) {
        /*
         * https://sanastot.dev.yti.cloud.vrk.fi/termed-api/graphs/5b3eb5d7-0239-484d-8515-
         * bc4b8cb42e7e/node-trees?select=*,references.prefLabelXl:1&where=type.id:
         * Concept%20OR%20type.id:Collection&max=-1
         * 
         */

        Parameters params = constructVocabularyTypeQuery(nodeTypes);
        String path = "/graphs/" + id.toString() + "/node-trees";
        // Execute full search
        JsonNode rv = requireNonNull(termedRequester.exchange(path, GET, params, JsonNode.class));
        return requireNonNull(rv);
    }

    @NotNull
    String getFullVocabularyRDF(UUID id) {
        Parameters params = this.constructFullVocabularyIdQuery(id);
        // Get XML-document back
        params.add("Content-Type", TermedContentType.RDF_XML.getContentType());
        String path = "/graphs/" + id.toString() + "/node-trees";
        // Execute full search
        String rv = requireNonNull(
                termedRequester.exchange(path, GET, params, String.class, TermedContentType.RDF_XML));
        return requireNonNull(rv);
    }

    @NotNull
    String getVocabularyRDF(UUID id, String nodeTypes) {
        Parameters params = constructVocabularyTypeQuery(nodeTypes);
        // Get XML-document back
        params.add("Content-Type", TermedContentType.RDF_XML.getContentType());
        String path = "/graphs/" + id.toString() + "/node-trees";
        // Execute full search
        String rv = requireNonNull(
                termedRequester.exchange(path, GET, params, String.class, TermedContentType.RDF_XML));
        return requireNonNull(rv);
    }

    @NotNull
    String getFullVocabularyTXT(UUID id) {
        Parameters params = this.constructFullVocabularyIdQuery(id);
        // Get XML-document back
        params.add("Content-Type", TermedContentType.RDF_TURTLE.getContentType());
        String path = "/graphs/" + id.toString() + "/node-trees";
        // Execute full search
        String rv = requireNonNull(
                termedRequester.exchange(path, GET, params, String.class, TermedContentType.RDF_TURTLE));
        return requireNonNull(rv);
    }

    @NotNull
    String getVocabularyTXT(UUID id, String nodeTypes) {
        Parameters params = this.constructVocabularyTypeQuery(nodeTypes);
        // Get XML-document back
        params.add("Content-Type", TermedContentType.RDF_TURTLE.getContentType());
        String path = "/graphs/" + id.toString() + "/node-trees";
        // Execute full search
        String rv = requireNonNull(
                termedRequester.exchange(path, GET, params, String.class, TermedContentType.RDF_TURTLE));
        return requireNonNull(rv);
    }

    /**
     * Get export data in JSON format and create Excel from it.
     */
    @NotNull
    ExcelCreator getFullVocabularyXLSX(UUID id) {
        JsonNode json = this.getFullVocabulary(id);
        List<JSONWrapper> wrappers = new ArrayList<>();
        json.forEach(node -> wrappers.add(new JSONWrapper(node)));

        return new ExcelCreator(wrappers);
    }

    ResponseEntity<String> getJSON(UUID vocabularyId) {
        return handleJSON(getFullVocabulary(vocabularyId));
    }

    ResponseEntity<String> getJSON(UUID vocabularyId, String nodeTypes) {
        return handleJSON(getVocabulary(vocabularyId, nodeTypes));
    }

    ResponseEntity<String> getRDF(UUID vocabularyId) {
        String response = getFullVocabularyRDF(vocabularyId);
        return buildOkResponse(response, TermedContentType.RDF_XML);
    }

    ResponseEntity<String> getRDF(UUID vocabularyId, String nodeType) {
        String response = getVocabularyRDF(vocabularyId, nodeType);
        return buildOkResponse(response, TermedContentType.RDF_XML);
    }

    ResponseEntity<String> getTXT(UUID vocabularyId) {
        String response = getFullVocabularyTXT(vocabularyId);
        return buildOkResponse(response, TermedContentType.RDF_TURTLE);
    }

    ResponseEntity<String> getTXT(UUID vocabularyId, String nodeType) {
        String response = getVocabularyTXT(vocabularyId, nodeType);
        return buildOkResponse(response, TermedContentType.RDF_TURTLE);
    }

    /**
     * Create excel and send it as a response.
     */
    ResponseEntity<InputStreamResource> getXLSX(UUID vocabularyId) {
        ExcelCreator creator = getFullVocabularyXLSX(vocabularyId);
        Workbook workbook = creator.createExcel();
        String filename = String.format(
                "%s_export_%s",
                creator.getFilename(),
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
        );

        return buildExcelResponse(workbook, filename);
    }

    private ResponseEntity<String> handleJSON(JsonNode response) {
        if (response == null || response.isNull()) {
            return buildResponse("null", TermedContentType.JSON, HttpStatus.NOT_FOUND);
        }
        String body = JsonUtils.prettyPrintJsonAsString(response);
        if (response.isArray() && response.size() == 0) {
            return buildResponse(body, TermedContentType.JSON, HttpStatus.NOT_FOUND);
        }
        return buildOkResponse(body, TermedContentType.JSON);
    }

    private ResponseEntity<String> buildOkResponse(String body, TermedContentType contentType) {
        return buildResponse(body, contentType, HttpStatus.OK);
    }

    private ResponseEntity<String> buildResponse(String body, TermedContentType contentType, HttpStatus status) {
        return ResponseEntity
            .status(status)
            .contentType(MediaType.valueOf(contentType.getContentType()))
            .body(body);
    }

    private ResponseEntity<InputStreamResource> buildExcelResponse(final Workbook workbook, final String filename) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            workbook.write(out);
        } catch (final Exception e) {
            logger.error("Excel output generation issue.", e);
            throw new InternalServerErrorException("Excel output generation failed!");
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename = " + filename + ".xlsx")
                .body(new InputStreamResource(in));
    }
}
