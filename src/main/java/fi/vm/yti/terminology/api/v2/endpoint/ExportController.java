package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.terminology.api.v2.service.TerminologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("v2/export")
@Tag(name = "Export")
public class ExportController {

    private final TerminologyService terminologyService;

    public ExportController(TerminologyService terminologyService) {
        this.terminologyService = terminologyService;
    }

    @Operation(summary = "Get terminology serialized")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get and serialize terminology successfully"),
            @ApiResponse(responseCode = "404", description = "Terminology not found")
    })
    @GetMapping(value = {"{prefix}"},
            produces = {"application/ld+json;charset=utf-8", "text/turtle;charset=utf-8", "application/rdf+xml;charset=utf-8"})
    public ResponseEntity<String> export(@PathVariable @Parameter(description = "Terminology prefix") String prefix,
                                         @RequestParam(required = false) @Parameter(description = "Type of the file") String fileType,
                                         @RequestHeader(value = HttpHeaders.ACCEPT) String accept) {
        var showAsFile = false;
        var type = accept;

        if (fileType != null) {
            type = URLDecoder.decode(fileType, StandardCharsets.UTF_8);
            showAsFile = true;
        }
        return terminologyService.export(prefix, type, showAsFile);
    }
}
