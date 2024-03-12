package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.dto.TerminologyInfoDTO;
import fi.vm.yti.terminology.api.v2.service.TerminologyService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("v2/terminology")
@Tag(name = "Terminology" )
public class TerminologyController {

    private final TerminologyService terminologyService;

    public TerminologyController(TerminologyService terminologyService) {
        this.terminologyService = terminologyService;
    }

    @GetMapping("/{prefix}")
    public TerminologyInfoDTO get(@PathVariable @Parameter(description = "Terminology prefix") String prefix) {
        return terminologyService.getTerminology(prefix);
    }

    @PostMapping
    public ResponseEntity<String> create() throws URISyntaxException {
        var dto = new TerminologyDTO();
        dto.setPrefix("test");
        dto.setOrganizations(Set.of(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")));
        var uri = terminologyService.creteTerminology(dto);
        return ResponseEntity.created(uri).build();
    }

    @PutMapping("/{prefix}")
    public ResponseEntity<Void> update(@PathVariable @Parameter(description = "Terminology prefix") String prefix) {
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{prefix}")
    public void delete(@PathVariable @Parameter(description = "Data model prefix") String prefix) {
        terminologyService.deleteTerminology(prefix);
    }
}
