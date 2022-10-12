package fi.vm.yti.terminology.api.importapi;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.importapi.excel.ExcelCreator;
import fi.vm.yti.terminology.api.importapi.excel.JSONWrapper;
import fi.vm.yti.terminology.api.security.AuthorizationTermedService;
import jakarta.ws.rs.InternalServerErrorException;
import org.apache.poi.ss.usermodel.Workbook;
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
    private AuthenticatedUserProvider userProvider;
    private AuthorizationTermedService authorizationTermedService;

    @Autowired
    public ExportService(TermedRequester termedRequester, AuthenticatedUserProvider userProvider,
                         AuthorizationTermedService authorizationTermedService) {
        this.termedRequester = termedRequester;
        this.userProvider = userProvider;
        this.authorizationTermedService = authorizationTermedService;
    }

    private Parameters constructFullVocabularyQuery() {
        return this.constructVocabularyTypeQuery("*, references.prefLabelXl:1", null);
    }

    private Parameters constructVocabularyTypeQuery(String nodeType) {
        return this.constructVocabularyTypeQuery("*,references.prefLabelXl:1", nodeType);
    }

    private Parameters constructVocabularyTypeQuery(@NotNull String select, String nodeType) {
        Parameters params = new Parameters();
        params.add("select", select);

        if (nodeType != null) {
            // Get all nodes from given graph
            if (nodeType.contains(",")) {
                params.add("where", "(type.id:" + nodeType.replaceAll(",", " OR type.id:") + ")");
            } else {
                params.add("where", "type.id:" + nodeType);
            }
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
        Parameters params = constructFullVocabularyQuery();
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

    /**
     * Get concepts of given vocabulary id but select only id, type and uri.
     */
    @NotNull
    JsonNode getConceptURIFromVocabulary(UUID id) {
        Parameters params = constructVocabularyTypeQuery("id,type,uri", "Concept");
        String path = "/graphs/" + id.toString() + "/node-trees";
        // Execute full search
        JsonNode rv = requireNonNull(termedRequester.exchange(path, GET, params, JsonNode.class));
        return requireNonNull(rv);
    }

    @NotNull
    String getFullVocabularyRDF(UUID id) {
        Parameters params = this.constructFullVocabularyQuery();
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
        Parameters params = this.constructFullVocabularyQuery();
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
        json.forEach(node -> wrappers.add(new JSONWrapper(node, wrappers)));

        this.fetchConceptsFromOtherGraphs(wrappers);

        return new ExcelCreator(wrappers);
    }

    /**
     * Loop over concept links and fetch their reference concepts from other vocabularies. The URI of that concept is
     * then stored as a memo to the original concept link.
     */
    private void fetchConceptsFromOtherGraphs(@NotNull List<JSONWrapper> wrappers) {
        List<JSONWrapper> conceptLinks = this.wrappersOfType(wrappers, "ConceptLink");
        Map<String, String> uris = getExternalURIs(conceptLinks);
        conceptLinks.forEach(link -> link.setMemo(uris.get(String.format(
                "%s/%s",
                link.getFirstPropertyValue("targetGraph", ""),
                link.getFirstPropertyValue("targetId", "")
        ))));
    }

    /**
     * Filter JSONWrappers by type.
     */
    private @NotNull List<JSONWrapper> wrappersOfType(@NotNull List<JSONWrapper> wrappers, @NotNull String type) {
        return wrappers.stream().filter(wrapper -> wrapper.getType().equals(type)).collect(Collectors.toList());
    }

    /**
     * Fetch external vocabularies linked in given concept links and map their content to "${graphId}/${id}": "${uri}"
     * pairs.
     */
    @NotNull
    private Map<String, String> getExternalURIs(List<JSONWrapper> conceptLinks) {
        Map<String, String> result = new HashMap<>();

        conceptLinks.stream()
                .map(link -> link.getFirstPropertyValue("targetGraph", ""))
                .collect(Collectors.toSet())
                .forEach(graphId -> {
                    JsonNode json = this.getConceptURIFromVocabulary(UUID.fromString(graphId));
                    json.forEach(node -> {
                        JSONWrapper wrapper = new JSONWrapper(node, List.of());
                        result.put(graphId + "/" + wrapper.getID(), wrapper.getURI());
                    });
                });

        return result;
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
    ResponseEntity<InputStreamResource> getXLSX(UUID vocabularyId, List<String> placeholderLanguages) {
        ExcelCreator creator = getFullVocabularyXLSX(vocabularyId);
        YtiUser user = userProvider.getUser();
        Set<UUID> organizationIds = authorizationTermedService.getOrganizationIds(vocabularyId);

        boolean isInOrganization = user.isSuperuser() || user.isInAnyRole(
                List.of(Role.TERMINOLOGY_EDITOR, Role.ADMIN), organizationIds);

        Workbook workbook;
        if (user.isSuperuser()) {
            workbook = creator.createExcel(placeholderLanguages, true);
        } else {
            workbook = creator.createExcel(isInOrganization);
        }

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
