package fi.vm.yti.terminology.api.v2.integration;

import fi.vm.yti.common.enums.Status;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(value = { "api/v1/integration", "v2/integration" })
@Tag(name = "Integration")
public class IntegrationController {

    private final IntegrationService service;

    public IntegrationController(IntegrationService service) {
        this.service = service;
    }

    @GetMapping("/containers")
    public IntegrationResponse getContainers() {
        return service.getContainers(Set.of());
    }

    @PostMapping("/containers")
    public IntegrationResponse getContainers(@RequestBody ContainerRequest request) {
        return service.getContainers(request.getUri());
    }


    @GetMapping("/resources")
    public IntegrationResponse getResources(
            @RequestParam(required = false) String containerUri,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Integer pageSize
    ) {
        var request = new ResourceRequest();
        if (containerUri != null) {
            request.setContainer(List.of(containerUri));
        }
        request.setSearchTerm(searchTerm);
        request.setAfter(after);
        request.setPageSize(pageSize);
        request.setStatus(status);
        return service.getContainerResources(request);
    }

    @PostMapping("/resources")
    public IntegrationResponse getResources(@RequestBody ResourceRequest request) {
        return service.getContainerResources(request);
    }

    public static class ContainerRequest {
        private Set<String> uri;

        public Set<String> getUri() {
            return uri;
        }

        public void setUri(Set<String> uri) {
            this.uri = uri;
        }
    }

    public static class ResourceRequest {
        private List<String> container = new ArrayList<>();
        private String after;
        private Integer pageSize;
        private String searchTerm;
        private Status status;

        public List<String> getContainer() {
            return container;
        }

        public void setContainer(List<String> container) {
            this.container = container;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(String after) {
            this.after = after;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        public String getSearchTerm() {
            return searchTerm;
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }
    }
}
