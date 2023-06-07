package fi.vm.yti.terminology.api.resolve;

import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.util.Parameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        ResolveService.class
})
@TestPropertySource(properties = {
        "namespace.root=http://uri.suomi.fi/terminology/"
})
class ResolveServiceTest {

    @MockBean
    TermedRequester termedRequester;

    @Autowired
    ResolveService resolveService;

    @ParameterizedTest
    @CsvSource({"http://uri.suomi.fi/terminology/test/", "http://uri.suomi.fi/terminology/test"})
    void resolveVocabularyTest() {
        var uuid = UUID.randomUUID();
        when(termedRequester.exchange(eq(TermedRequester.PATH_GRAPHS), eq(HttpMethod.GET), any(Parameters.class), any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(new Graph(
                        uuid,
                        "test",
                        "http://uri.suomi.fi/terminology/test",
                        emptyList(),
                        emptyMap(),
                        emptyMap()
                )));

        var result = resolveService.resolveResource("http://uri.suomi.fi/terminology/test/");

        verify(termedRequester).exchange(eq(TermedRequester.PATH_GRAPHS), eq(HttpMethod.GET), any(Parameters.class), any(ParameterizedTypeReference.class));
        verifyNoMoreInteractions(termedRequester);
        assertEquals(ResolvedResource.Type.VOCABULARY, result.getType());
        assertEquals(uuid, result.getGraphId());
        assertNull(result.getId());
    }

    @Test
    void resolveResourceTest() {
        var uuid = UUID.randomUUID();
        when(termedRequester.exchange(eq(TermedRequester.PATH_GRAPHS), eq(HttpMethod.GET), any(Parameters.class), any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(new Graph(
                        uuid,
                        "test",
                        "http://uri.suomi.fi/terminology/test",
                        emptyList(),
                        emptyMap(),
                        emptyMap()
                )));
        when(termedRequester.exchange(eq(TermedRequester.PATH_NODE_TREES), eq(HttpMethod.GET), any(Parameters.class), any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(new GenericNode(
                        new TypeId(NodeType.Concept, new GraphId(uuid)),
                        Collections.emptyMap(),
                        Collections.emptyMap()
                )));

        var result = resolveService.resolveResource("http://uri.suomi.fi/terminology/test/concept-0");
        verify(termedRequester).exchange(eq(TermedRequester.PATH_GRAPHS), eq(HttpMethod.GET), any(Parameters.class), any(ParameterizedTypeReference.class));
        verify(termedRequester).exchange(eq(TermedRequester.PATH_NODE_TREES), eq(HttpMethod.GET), any(Parameters.class), any(ParameterizedTypeReference.class));
        verifyNoMoreInteractions(termedRequester);
        assertEquals(ResolvedResource.Type.CONCEPT, result.getType());
        assertEquals(uuid, result.getGraphId());
        assertNotNull(result.getId()); //this is random uuid so we can only check nullness
    }

    @Test
    void resolveInvalidUriTest(){
        assertThrows(ResolveService.ResolveException.class, () -> resolveService.resolveResource("invalid"));
    }
}
