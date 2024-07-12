package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.common.repository.CommonRepository;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.exception.TerminologyExceptionHandlerAdvice;
import fi.vm.yti.terminology.api.v2.opensearch.ConceptSearchRequest;
import fi.vm.yti.terminology.api.v2.opensearch.TerminologySearchRequest;
import fi.vm.yti.terminology.api.v2.service.SearchIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(controllers = FrontendController.class)
@ActiveProfiles("test")
public class FrontendControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GroupManagementService groupManagementService;

    @MockBean
    private CommonRepository commonRepository;

    @Autowired
    private FrontendController frontendController;

    @MockBean
    private SearchIndexService searchIndexService;

    @MockBean
    private AuthenticatedUserProvider userProvider;

    @BeforeEach
    void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.frontendController)
                .setControllerAdvice(new TerminologyExceptionHandlerAdvice())
                .build();

        when(userProvider.getUser()).thenReturn(TestUtils.mockUser);
    }

    @Test
    void shouldSearchTerminologies() throws Exception {
        this.mvc.perform(get("/v2/frontend/search-terminologies")
                        .contentType("application/json"))
                .andExpect(status().isOk());
        verify(searchIndexService)
                .searchTerminologies(any(TerminologySearchRequest.class), any(YtiUser.class));
    }

    @Test
    void shouldSearchConcepts() throws Exception {
        this.mvc.perform(get("/v2/frontend/search-concepts")
                        .contentType("application/json"))
                .andExpect(status().isOk());
        verify(searchIndexService)
                .searchConcepts(any(ConceptSearchRequest.class), any(YtiUser.class));
    }
}
