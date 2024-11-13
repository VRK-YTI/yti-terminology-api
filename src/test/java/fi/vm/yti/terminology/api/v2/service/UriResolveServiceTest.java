package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        UriResolveService.class
})
class UriResolveServiceTest {

    @MockBean
    private TerminologyRepository repository;

    @Autowired
    private UriResolveService service;

    @BeforeEach
    void init() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void redirectTerminologyToSite() {
        var uri = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var response = service.resolve(uri, "text/html");

        assertTrue(response.getStatusCode().is3xxRedirection());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/terminology/test"));
    }

    @Test
    void redirectResourceToSite() {
        var uri = TerminologyURI.createConceptURI("test", "concept-1").getResourceURI();
        mockGetResourceType();

        var response = service.resolve(uri, "text/html");

        assertTrue(response.getStatusCode().is3xxRedirection());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/terminology/test/concept/concept-1"));
    }

    @Test
    void redirectTerminologyToExport() {
        var uri = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var response = service.resolve(uri, "text/turtle");

        assertTrue(response.getStatusCode().is3xxRedirection());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/terminology-api/v2/export/test"));
    }

    @Test
    void redirectResourceIdStartingWithDigit() {
        var uri = TerminologyURI.createConceptURI("1234abc", "1234").getResourceURI();
        mockGetResourceType();

        var response = service.resolve(uri, "text/html");

        assertTrue(response.getStatusCode().is3xxRedirection());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/terminology/a1234abc/concept/a1234"));
    }

    private void mockGetResourceType() {
        var qs = mock(QuerySolution.class);
        when(qs.get("type")).thenReturn(ResourceFactory.createStringLiteral(SKOS.Concept.getURI()));

        doAnswer(ans -> {
            Consumer<QuerySolution> cons = ans.getArgument(1, Consumer.class);
            cons.accept(qs);
            return null;
        }).when(repository).querySelect(any(Query.class), any(Consumer.class));
    }
}
