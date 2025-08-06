package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.dto.OrganizationDTO;
import fi.vm.yti.common.dto.ServiceCategoryDTO;
import fi.vm.yti.common.opensearch.SearchResponseDTO;
import fi.vm.yti.common.service.FrontendService;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.v2.dto.ConceptSearchResultDTO;
import fi.vm.yti.terminology.api.v2.dto.CountSearchResponse;
import fi.vm.yti.terminology.api.v2.dto.StatusCountResponse;
import fi.vm.yti.terminology.api.v2.dto.TerminologySearchResultDTO;
import fi.vm.yti.terminology.api.v2.opensearch.ConceptSearchRequest;
import fi.vm.yti.terminology.api.v2.opensearch.TerminologySearchRequest;
import fi.vm.yti.terminology.api.v2.service.SearchIndexService;
import fi.vm.yti.terminology.api.v2.service.TerminologyService;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/frontend")
@Tag(name = "Frontend")
public class FrontendController {

    private final SearchIndexService searchIndexService;

    private final FrontendService frontendService;

    private final AuthenticatedUserProvider userProvider;

    private final TerminologyService terminologyService;

    public FrontendController(
            SearchIndexService searchIndexService,
            FrontendService frontendService,
            AuthenticatedUserProvider userProvider,
            TerminologyService terminologyService) {
        this.frontendService = frontendService;
        this.searchIndexService = searchIndexService;
        this.userProvider = userProvider;
        this.terminologyService = terminologyService;
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

    @Operation(summary = "Search terminologies")
    @ApiResponse(responseCode = "200", description = "List of terminology objects")
    @GetMapping(value = "/search-terminologies", produces = APPLICATION_JSON_VALUE)
    public SearchResponseDTO<TerminologySearchResultDTO> getTerminologies(
            @Parameter(description = "Terminology search parameters") TerminologySearchRequest request
    ) {
        return searchIndexService.searchTerminologies(request, userProvider.getUser());
    }

    @Operation(summary = "Search concepts")
    @ApiResponse(responseCode = "200", description = "List of concept objects")
    @GetMapping(value = "/search-concepts", produces = APPLICATION_JSON_VALUE)
    public SearchResponseDTO<ConceptSearchResultDTO> getConcepts(
            @Parameter(description = "Concept search parameters") ConceptSearchRequest request
    ) {
        return searchIndexService.searchConcepts(request, userProvider.getUser());
    }

    @Operation(summary = "Get frontpage counts")
    @ApiResponse(responseCode = "200", description = "")
    @GetMapping(value = "/counts", produces = APPLICATION_JSON_VALUE)
    public CountSearchResponse getCounts(
            @Parameter(description = "Terminology search parameters") TerminologySearchRequest request) {
        return searchIndexService.getTerminologyCounts(request, userProvider.getUser());
    }

    @Operation(summary = "Get concept counts")
    @ApiResponse(responseCode = "200", description = "")
    @GetMapping(value = "/concept-counts", produces = APPLICATION_JSON_VALUE)
    public CountSearchResponse getConceptCounts(@RequestParam String prefix) {
        var request = new ConceptSearchRequest();
        request.setNamespace(TerminologyURI.Factory.createTerminologyURI(prefix).getGraphURI());
        request.setQuery("");
        return searchIndexService.getConceptCounts(request, userProvider.getUser());
    }

    @Operation(summary = "Get status counts")
    @ApiResponse(responseCode = "200", description = "")
    @GetMapping(value = "/status-counts", produces = APPLICATION_JSON_VALUE)
    public StatusCountResponse getStatusCounts(@RequestParam String prefix) {
        return terminologyService.getCountsByStatus(
                TerminologyURI.Factory.createTerminologyURI(prefix).getGraphURI());
    }
}
