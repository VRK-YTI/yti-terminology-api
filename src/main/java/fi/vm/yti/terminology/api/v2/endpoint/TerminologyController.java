package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.common.dto.MetaDataDTO;
import fi.vm.yti.common.dto.MetaDataInfoDTO;
import fi.vm.yti.terminology.api.v2.service.TerminologyService;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import fi.vm.yti.terminology.api.v2.validator.ValidTerminology;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/terminology")
@Tag(name = "Terminology")
@Validated
public class TerminologyController {

    private final TerminologyService terminologyService;

    public TerminologyController(TerminologyService terminologyService) {
        this.terminologyService = terminologyService;
    }

    @Operation(summary = "Get terminology metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Terminology fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Terminology not found",
                    content = {@Content(mediaType = "application/json")}),
    })
    @GetMapping(path = "/{prefix}", produces = APPLICATION_JSON_VALUE)
    public MetaDataInfoDTO get(@PathVariable @Parameter(description = "Terminology prefix") String prefix) {
        return terminologyService.get(prefix);
    }

    @Operation(summary = "Create a new terminology")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for new terminology metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The URI for the newly created terminology"),
            @ApiResponse(responseCode = "400", description = "One or more of the fields in the JSON data was invalid or malformed",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights to create terminology"),
    })
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> create(@RequestBody @ValidTerminology MetaDataDTO terminology) throws URISyntaxException {
        var uri = terminologyService.create(terminology);
        return ResponseEntity.created(uri).build();
    }

    @Operation(summary = "Modify terminology metadata")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for updated terminology metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Library updated successfully"),
            @ApiResponse(responseCode = "400", description = "One or more of the fields in the JSON data was invalid or malformed",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this terminology"),
            @ApiResponse(responseCode = "404", description = "Terminology was not found", content = {@Content(mediaType = "application/json")})
    })
    @PutMapping(path = "/{prefix}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> update(
            @PathVariable @Parameter(description = "Terminology prefix") String prefix,
            @RequestBody @ValidTerminology(update = true) MetaDataDTO terminology) {
        terminologyService.update(prefix, terminology);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete terminology")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Terminology deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this terminology"),
            @ApiResponse(responseCode = "404", description = "Terminology was not found",
                    content = {@Content(mediaType = "application/json")}),
    })
    @DeleteMapping("/{prefix}")
    public ResponseEntity<Void> delete(@PathVariable @Parameter(description = "Terminology prefix") String prefix) {
        terminologyService.delete(prefix);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check if prefix already exists")
    @ApiResponse(responseCode = "200", description = "Boolean value indicating whether prefix")
    @GetMapping(value = "/{prefix}/exists", produces = APPLICATION_JSON_VALUE)
    public Boolean exists(@PathVariable @Parameter(description = "Data model prefix") String prefix) {
        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();
        return terminologyService.exists(graphURI);
    }
}
