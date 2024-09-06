package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionInfoDTO;
import fi.vm.yti.terminology.api.v2.service.ConceptCollectionService;
import fi.vm.yti.terminology.api.v2.validator.ValidConceptCollection;
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
@RequestMapping("v2/collection")
@Tag(name = "ConceptCollection")
@Validated
public class ConceptCollectionController {
    private final ConceptCollectionService conceptCollectionService;

    public ConceptCollectionController(ConceptCollectionService conceptCollectionService) {
        this.conceptCollectionService = conceptCollectionService;
    }

    @Operation(summary = "Get add concept collection in terminology")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Concept collections fetched successfully"),
    })
    @GetMapping(path = "/{prefix}")
    public List<ConceptCollectionInfoDTO> list(@PathVariable @Parameter(description = "Terminology prefix") String prefix) {
        return conceptCollectionService.list(prefix);
    }

    @Operation(summary = "Get concept collection information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Concept collection fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Concept collection not found",
                    content = {@Content(mediaType = APPLICATION_JSON_VALUE)}),
    })
    @GetMapping(path = "/{prefix}/{collectionIdentifier}", produces = APPLICATION_JSON_VALUE)
    public ConceptCollectionInfoDTO get(@PathVariable @Parameter(description = "Terminology prefix") String prefix,
                                        @PathVariable @Parameter(description = "Collection identifier") String collectionIdentifier) {
        return conceptCollectionService.get(prefix, collectionIdentifier);
    }

    @Operation(summary = "Create a new concept collection to terminology")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for concept collection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The URI for the newly created concept collection"),
            @ApiResponse(responseCode = "400", description = "One or more of the fields in the JSON data was invalid or malformed",
                    content = {@Content(mediaType = APPLICATION_JSON_VALUE)}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights to create concept collection"),
    })
    @PostMapping(path = "/{prefix}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> create(@PathVariable String prefix,
                                         @RequestBody @ValidConceptCollection ConceptCollectionDTO conceptCollection) throws URISyntaxException {
        var conceptCollectionURI = conceptCollectionService.create(prefix, conceptCollection);
        return ResponseEntity.created(conceptCollectionURI).build();
    }

    @Operation(summary = "Modify concept collection")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for updated concept collection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Concept collection updated successfully"),
            @ApiResponse(responseCode = "400", description = "One or more of the fields in the JSON data was invalid or malformed",
                    content = {@Content(mediaType = APPLICATION_JSON_VALUE)}),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this terminology"),
            @ApiResponse(responseCode = "404", description = "Terminology or concept collection was not found", content = {@Content(mediaType = APPLICATION_JSON_VALUE)})
    })
    @PutMapping(path = "/{prefix}/{conceptCollectionIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> update(
            @PathVariable @Parameter(description = "Terminology prefix") String prefix,
            @PathVariable @Parameter(description = "Concept collection identifier") String conceptCollectionIdentifier,
            @RequestBody @ValidConceptCollection(update = true) ConceptCollectionDTO conceptCollection) {
        conceptCollectionService.update(prefix, conceptCollectionIdentifier, conceptCollection);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete concept collection from terminology")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Concept collection deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this terminology"),
            @ApiResponse(responseCode = "404", description = "Terminology or concept collection was not found",
                    content = {@Content(mediaType = APPLICATION_JSON_VALUE)}),
    })
    @DeleteMapping("/{prefix}/{conceptCollectionIdentifier}")
    public ResponseEntity<Void> delete(@PathVariable @Parameter(description = "Terminology prefix") String prefix,
                                       @PathVariable @Parameter(description = "Concept collection identifier") String conceptCollectionIdentifier) {
        conceptCollectionService.delete(prefix, conceptCollectionIdentifier);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check if concept collection with identifier exists in terminology")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Boolean value indicating whether concept collection exist")
    })
    @GetMapping("/{prefix}/{conceptCollectionIdentifier}/exists")
    public Boolean exists(@PathVariable @Parameter(description = "Terminology prefix") String prefix,
                          @PathVariable @Parameter(description = "Concept collection identifier") String conceptCollectionIdentifier) {
        return conceptCollectionService.exists(
                prefix,
                conceptCollectionIdentifier);
    }
}
