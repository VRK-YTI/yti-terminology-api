package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.terminology.api.v2.service.NTRFImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("v2/import")
@Tag(name = "Import")
public class ImportController {

    private final NTRFImportService ntrfImportService;

    public ImportController(NTRFImportService ntrfImportService) {
        this.ntrfImportService = ntrfImportService;
    }

    @Operation(summary = "NTRF import", description = "Import concepts from a NTRF (XML) document")
    @ApiResponse(responseCode = "200")
    @PostMapping(path = "ntrf/{prefix}", consumes = MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Void> importTerms(@Parameter(description = "Terminology prefix") @PathVariable("prefix") String prefix,
                                     @Parameter(required = true, description = "The NTRF (XML) document containing the concepts to be imported", style = ParameterStyle.FORM)
                                     @RequestPart(value = "file") MultipartFile file) {
        ntrfImportService.importNTRF(prefix, file);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Initiate simple Excel import job", description = "Start the procedure to import concepts from Excel file")
    @ApiResponse(
            responseCode = "200",
            description = "If import process started successfully then job token is returned as JSON")
    @PostMapping(path = "simpleExcel/{prefix}", consumes = MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Void> importSimpleExcel(@Parameter(description = "The ID of the terminology to import concepts to")
                                                             @PathVariable("prefix") String prefix,
                                                             @RequestPart(value = "file") MultipartFile file) {
        // TODO: implement simple excel import
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
