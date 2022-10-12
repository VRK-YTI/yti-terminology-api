package fi.vm.yti.terminology.api.resolve;

import fi.vm.yti.terminology.api.ExceptionHandlerAdvice;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false",
        "application.public.url=https://sanastot.test.yti.cloud.dvv.fi",
        "application.public.beta.url=https://yhteentoimiva.test.yti.cloud.dvv.fi"
})
@WebMvcTest(controllers = ResolveController.class)
public class ResolveControllerTest {

    public static final String URI = "https://uri.suomi.fi/terminology/abc123";

    @Autowired
    private ResolveController resolveController;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ResolveService urlResolverService;

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.resolveController)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();
    }

    @Test
    public void testResolveURIWithoutEnv() throws Exception {
        mockResolveResource();

        this.mvc.perform(getRequest(null, URI))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        Matchers.startsWith("https://sanastot.test.yti.cloud.dvv.fi")));
    }

    @Test
    public void testResolveBetaURI() throws Exception {
        mockResolveResource();

        this.mvc.perform(getRequest("awstest_v2", URI))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        Matchers.startsWith("https://yhteentoimiva.test.yti.cloud.dvv.fi")));
    }

    @Test
    public void testResolveTestURI() throws Exception {
        mockResolveResource();

        this.mvc.perform(getRequest("awstest", URI))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        Matchers.startsWith("https://sanastot.test.yti.cloud.dvv.fi")));
    }

    @Test
    public void testInvalidURI() throws Exception {
        this.mvc.perform(getRequest(null, "invalid"))
            .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder getRequest(String env, String uri) {
        MockHttpServletRequestBuilder builder = get("/api/v1/resolve")
                .param("uri", uri)
                .accept("*/*")
                .contentType("application/json");
        if (env != null) {
            builder.param("env", env);
        }

        return builder;
    }

    private void mockResolveResource() {
        UUID terminologyId = UUID.randomUUID();
        ResolvedResource resource = new ResolvedResource(terminologyId, ResolvedResource.Type.VOCABULARY);

        when(urlResolverService.resolveResource(anyString()))
                .thenReturn(resource);
    }
}
