package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.service.TerminologyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v2/terminology")
@Tag(name = "Terminology" )
public class TerminologyController {

    private final TerminologyService terminologyService;

    public TerminologyController(TerminologyService terminologyService) {
        this.terminologyService = terminologyService;
    }

    @PostMapping
    void create() {
        var dto = new TerminologyDTO();
        dto.setPrefix("test");
        terminologyService.creteTerminology(dto);
    }
}
