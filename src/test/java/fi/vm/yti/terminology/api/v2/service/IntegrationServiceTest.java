package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.common.opensearch.OpenSearchUtil;
import fi.vm.yti.common.opensearch.SearchResponseDTO;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.integration.IntegrationController;
import fi.vm.yti.terminology.api.v2.integration.IntegrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import org.opensearch.client.opensearch.core.SearchRequest;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        IntegrationService.class,
})
class IntegrationServiceTest {

    @MockBean
    private OpenSearchClientWrapper client;

    @Autowired
    IntegrationService service;

    @Test
    void testGetResources() throws Exception {
        var response = new SearchResponseDTO<>();
        response.setResponseObjects(List.of());

        when(client.search(any(SearchRequest.class), any()))
                .thenReturn(response);

        var request = new IntegrationController.ResourceRequest();
        request.setAfter("2024-07-24T00:00:00");
        request.setBefore("2024-07-25T00:00:00");
        request.setStatus(Status.DRAFT);
        request.setPageSize(50);
        request.setContainer(List.of("https://iri.suomi.fi/terminology/test/"));
        request.setSearchTerm("Search");

        var expected = TestUtils.getJsonString("/integration/resource-search-request.json");

        var captor = ArgumentCaptor.forClass(SearchRequest.class);
        service.getContainerResources(request);

        verify(client).search(captor.capture(), any());
        JSONAssert.assertEquals(expected, OpenSearchUtil.getPayload(captor.getValue()), JSONCompareMode.LENIENT);
    }

    @Test
    void testGetContainers() throws Exception {
        var response = new SearchResponseDTO<>();
        response.setResponseObjects(List.of());

        when(client.search(any(SearchRequest.class), any()))
                .thenReturn(response);

        var request = new IntegrationController.ContainerRequest();
        request.setUri(Set.of("https://iri.suomi.fi/terminology/test/"));

        var expected = TestUtils.getJsonString("/integration/container-search-request.json");

        var captor = ArgumentCaptor.forClass(SearchRequest.class);

        service.getContainers(request);

        verify(client).search(captor.capture(), any());
        JSONAssert.assertEquals(expected, OpenSearchUtil.getPayload(captor.getValue()), JSONCompareMode.LENIENT);
    }
}
