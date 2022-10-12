package fi.vm.yti.terminology.service;

import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.security.DefaultAuthorizationTermedService;
import fi.vm.yti.terminology.api.util.Parameters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

@ExtendWith(SpringExtension.class)
@Import({
        DefaultAuthorizationTermedService.class,
})
@TestPropertySource(properties = {
        "termed.cache.expiration=3"
})
public class DefaultAuthorizationTermedServiceTest {

    @MockBean
    TermedRequester requester;

    @Autowired
    DefaultAuthorizationTermedService service;

    private UUID organizationId = UUID.fromString("9adc94fb-65ab-4fd1-a9ef-de126b26cbe6");

    @Test
    public void cachedOrganizations() {
        UUID graphId = UUID.randomUUID();
        mockTermedRequest(graphId);

        Set<UUID> ids = service.getOrganizationIds(graphId);
        service.getOrganizationIds(graphId);

        // expect only one requester interaction, because result is cached
        verify(requester).exchange(eq(getPath(graphId)),
                eq(HttpMethod.GET),
                any(Parameters.class),
                any(ParameterizedTypeReference.class));
        assertEquals(organizationId, ids.iterator().next());
    }

    @Test
    public void cachedOrganizationsExpired() throws Exception {
        UUID graphId = UUID.randomUUID();
        mockTermedRequest(graphId);

        Set<UUID> ids = service.getOrganizationIds(graphId);

        // Wait for cache expiration
        Thread.sleep(4000);

        service.getOrganizationIds(graphId);

        // expect two requester interactions, because cache has been expired
        verify(requester, times(2)).exchange(eq(getPath(graphId)),
                eq(HttpMethod.GET),
                any(Parameters.class),
                any(ParameterizedTypeReference.class));

        assertEquals(organizationId, ids.iterator().next());
    }

    private List<GenericNode> getSampleNode() {
        var node = new GenericNode(
                UUID.randomUUID(),
                null,
                null,
                1L,
                "creator",
                new Date(),
                "modifier",
                new Date(),
                new TypeId(
                        NodeType.TerminologicalVocabulary,
                        new GraphId(UUID.randomUUID())),
                Collections.emptyMap(),
                Map.of("contributor",
                        List.of(new Identifier(
                                organizationId,
                                        new TypeId(
                                                NodeType.Organization,
                                                new GraphId(UUID.randomUUID())
                                        )
                                )
                        )
                ),
                Collections.emptyMap()
        );

        return Arrays.asList(node);
    }

    private void mockTermedRequest(UUID graphId) {
        when(requester
                .exchange(eq(getPath(graphId)),
                        eq(HttpMethod.GET),
                        any(Parameters.class),
                        any(ParameterizedTypeReference.class)
                )
        ).thenReturn(getSampleNode());
    }

    private String getPath(UUID graphId) {
        return String.format("/graphs/%s/types/%s/nodes", graphId, NodeType.TerminologicalVocabulary);
    }
}
