package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.common.enums.Status;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptInfoDTO;
import fi.vm.yti.terminology.api.v2.service.ConceptService;
import fi.vm.yti.terminology.api.v2.validator.ValidConcept;
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
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/concept")
@Tag(name = "Concept")
@Validated
public class ConceptController {

    private final ConceptService conceptService;

    public ConceptController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    @Operation(summary = "Get concept information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Concept fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Concept not found",
                    content = {@Content(mediaType = APPLICATION_JSON_VALUE)}),
    })
    @GetMapping(path = "/{prefix}/{conceptIdentifier}", produces = APPLICATION_JSON_VALUE)
    public ConceptInfoDTO get(@PathVariable @Parameter(description = "Terminology prefix") String prefix,
                              @PathVariable @Parameter(description = "Concept identifier") String conceptIdentifier) {
        return conceptService.get(prefix, conceptIdentifier);
    }

    @Operation(summary = "Create a new concept to terminology")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for concept")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The URI for the newly created concept"),
            @ApiResponse(responseCode = "400", description = "One or more of the fields in the JSON data was invalid or malformed",
                    content = {@Content(mediaType = APPLICATION_JSON_VALUE)}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights to create concept"),
    })
    @PostMapping(path = "/{prefix}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> create(@PathVariable String prefix,
                                         @RequestBody @ValidConcept ConceptDTO concept) throws URISyntaxException {
        var conceptURI = conceptService.create(prefix, concept);
        return ResponseEntity.created(conceptURI).build();
    }

    @Operation(summary = "Modify concept")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for updated concept")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Concept updated successfully"),
            @ApiResponse(responseCode = "400", description = "One or more of the fields in the JSON data was invalid or malformed",
                    content = {@Content(mediaType = APPLICATION_JSON_VALUE)}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this terminology"),
            @ApiResponse(responseCode = "404", description = "Terminology or concept was not found", content = {@Content(mediaType = APPLICATION_JSON_VALUE)})
    })
    @PutMapping(path = "/{prefix}/{conceptIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> update(
            @PathVariable @Parameter(description = "Terminology prefix") String prefix,
            @PathVariable @Parameter(description = "Concept identifier") String conceptIdentifier,
            @RequestBody @ValidConcept(update = true) ConceptDTO concept) {
        conceptService.update(prefix, conceptIdentifier, concept);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete concept from terminology")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Concept deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this terminology"),
            @ApiResponse(responseCode = "404", description = "Terminology or concept was not found",
                    content = {@Content(mediaType = APPLICATION_JSON_VALUE)}),
    })
    @DeleteMapping("/{prefix}/{conceptIdentifier}")
    public ResponseEntity<Void> delete(@PathVariable @Parameter(description = "Terminology prefix") String prefix,
                                       @PathVariable @Parameter(description = "Concept identifier") String conceptIdentifier) {
        conceptService.delete(prefix, conceptIdentifier);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check if concept with identifier exists in terminology")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Concept deleted successfully")
    })
    @GetMapping("/{prefix}/{conceptIdentifier}/exists")
    public Boolean exists(@PathVariable @Parameter(description = "Terminology prefix") String prefix,
                                       @PathVariable @Parameter(description = "Concept identifier") String conceptIdentifier) {
        return conceptService.exists(prefix, conceptIdentifier);
    }

    @Operation(summary = "Bulk change resources' status")
    @ApiResponse(responseCode = "200", description = "Change resources' status from status A to status B")
    @PostMapping(value = "/{prefix}/modify-statuses")
    public ResponseEntity<Void> modifyStatuses(
            @PathVariable String prefix,
            @RequestParam Status oldStatus,
            @RequestParam Status newStatus,
            @RequestParam List<String> types
    ) {
        conceptService.changeStatuses(prefix, oldStatus, newStatus, types);
        return ResponseEntity.noContent().build();
    }
}
