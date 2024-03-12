package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.dto.OrganizationDTO;
import fi.vm.yti.common.dto.ServiceCategoryDTO;
import fi.vm.yti.common.service.FrontendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("v2/frontend")
@Tag(name = "Frontend")
public class FrontendController {

    private final FrontendService frontendService;

    public FrontendController(FrontendService frontendService) {
        this.frontendService = frontendService;
    }

    @Operation(summary = "Get organizations", description = "List of organizations sorted by name")
    @ApiResponse(responseCode = "200", description = "Organization list as JSON")
    @GetMapping(path = "/organizations", produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<OrganizationDTO> getOrganizations(
            @RequestParam(required = false, defaultValue = Constants.DEFAULT_LANGUAGE)
            @Parameter(description = "Alphabetical sorting language") String sortLang,
            @RequestParam(required = false)
            @Parameter(description = "Include child organizations in response") boolean includeChildOrganizations) {
        return frontendService.getOrganizations(sortLang, includeChildOrganizations);
    }

    @Operation(summary = "Get service categories", description = "List of service categories sorted by name")
    @ApiResponse(responseCode = "200", description = "Service categories as JSON")
    @GetMapping(path = "/service-categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<ServiceCategoryDTO> getServiceCategories(
            @RequestParam(required = false, defaultValue = Constants.DEFAULT_LANGUAGE)
            @Parameter(description = "Alphabetical sorting language") String sortLang) {
        return frontendService.getServiceCategories(sortLang);
    }
}
