package fi.vm.yti.terminology.api.v2.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.common.dto.MetaDataDTO;
import fi.vm.yti.common.dto.OrganizationDTO;
import fi.vm.yti.common.dto.ServiceCategoryDTO;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.repository.CommonRepository;
import fi.vm.yti.common.service.FrontendService;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.validator.ValidationConstants;
import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.dto.TerminologyInfoDTO;
import fi.vm.yti.terminology.api.v2.exception.TerminologyExceptionHandlerAdvice;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.service.TerminologyService;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TerminologyController.class)
@ActiveProfiles("test")
class TerminologyControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CommonRepository commonRepository;

    @MockBean
    private GroupManagementService groupManagementService;

    @MockBean
    private TerminologyService terminologyService;

    @MockBean
    private TerminologyRepository terminologyRepository;

    @MockBean
    private FrontendService frontendService;

    @Autowired
    TerminologyController terminologyController;

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    private static final String VALID_GRAPH_URI = TerminologyURI.createTerminologyURI("test").getGraphURI();
    private static final String EXISTING_GRAPH = TerminologyURI.createTerminologyURI("existing-graph").getGraphURI();
    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.terminologyController)
                .setControllerAdvice(new TerminologyExceptionHandlerAdvice())
                .build();

        var o = new OrganizationDTO(ORGANIZATION_ID.toString(), Map.of("en", "Test organization"), null);
        var s = new ServiceCategoryDTO("abc123", Map.of("en", "Category"), "P10");
        when(frontendService.getOrganizations(anyString(), anyBoolean())).thenReturn(List.of(o));
        when(frontendService.getServiceCategories(anyString())).thenReturn(List.of(s));
        when(terminologyRepository.graphExists(VALID_GRAPH_URI)).thenReturn(false);
        when(terminologyRepository.graphExists(EXISTING_GRAPH)).thenReturn(true);
    }

    @Test
    void shouldValidateAndCreate() throws Exception {
        var terminologyDTO = getValidTerminologyMetadata();

        this.mvc
                .perform(post("/v2/terminology")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(terminologyDTO)))
                .andExpect(status().isCreated());

        verify(terminologyService).create(any(TerminologyDTO.class));
        verifyNoMoreInteractions(this.terminologyService);
    }

    @Test
    void shouldValidateAndUpdate() throws Exception {
        var terminologyDTO = getValidTerminologyMetadata();
        terminologyDTO.setPrefix(null);

        this.mvc
                .perform(put("/v2/terminology/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(terminologyDTO)))
                .andExpect(status().isNoContent());

        verify(terminologyService).update(eq("test"), any(TerminologyDTO.class));
        verifyNoMoreInteractions(this.terminologyService);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidCreateData")
    void shouldInvalidateOnCreation(DataWithError dto) throws Exception {
        this.mvc
                .perform(post("/v2/terminology")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto.data())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(dto.errorMessage())));

        verifyNoMoreInteractions(this.terminologyService);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidEditData")
    void shouldInvalidateOnUpdate(DataWithError data) throws Exception {
        this.mvc
                .perform(put("/v2/terminology/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(data.data())))
                .andExpect(content().string(containsString(data.errorMessage())))
                .andExpect(status().isBadRequest());

        verifyNoMoreInteractions(this.terminologyService);
    }

    @Test
    void shouldGetTerminologyData() throws Exception {
        var dto = new TerminologyInfoDTO();
        dto.setPrefix("test");
        dto.setLabel(Map.of("en", "Test terminology"));

        when(terminologyService.get("test")).thenReturn(dto);

        this.mvc
                .perform(get("/v2/terminology/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test terminology")));
    }

    @Test
    void shouldGetDataNotFound() throws Exception {
        when(terminologyService.get("test")).thenThrow(ResourceNotFoundException.class);
        this.mvc
                .perform(get("/v2/terminology/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteTerminology() throws Exception {
        this.mvc
                .perform(delete("/v2/terminology/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(terminologyService).delete("test");
    }

    private String asJsonString(Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

    private static Stream<Arguments> provideInvalidEditData() {
        var args = provideInvalidData();
        args.forEach(a -> a.data().setPrefix(null));

        var dto = getValidTerminologyMetadata();

        dto.setPrefix("test");
        args.add(new DataWithError("not-allowed-update", dto));

        return args.stream().map(Arguments::of);
    }

    private static Stream<Arguments> provideInvalidCreateData() {

        var args = provideInvalidData();

        var dto = getValidTerminologyMetadata();
        dto.setPrefix(null);
        args.add(new DataWithError("should-have-value", dto));

        dto = getValidTerminologyMetadata();
        dto.setPrefix("existing-graph");
        args.add(new DataWithError("prefix-in-use", dto));

        dto = getValidTerminologyMetadata();
        dto.setPrefix(RandomStringUtils.randomAlphabetic(ValidationConstants.PREFIX_MAX_LENGTH + 1).toLowerCase());
        args.add(new DataWithError("prefix-character-count-mismatch", dto));

        dto = getValidTerminologyMetadata();
        dto.setPrefix("ef#4");
        args.add(new DataWithError("invalid-value", dto));

        return args.stream().map(Arguments::of);
    }

    private static ArrayList<DataWithError> provideInvalidData() {

        var args = new ArrayList<DataWithError>();

        var dto = getValidTerminologyMetadata();

        // label validation
        dto.setLabel(Map.of("en", RandomStringUtils.randomAlphanumeric(ValidationConstants.TEXT_FIELD_MAX_LENGTH + 1)));
        args.add(new DataWithError("value-over-character-limit.150", dto));

        dto = getValidTerminologyMetadata();
        dto.setLabel(Map.of("en", ""));
        args.add(new DataWithError("should-have-value", dto));

        dto = getValidTerminologyMetadata();
        dto.setLabel(Map.of("es", "invalid language"));
        args.add(new DataWithError("language-not-in-language-list.es", dto));

        dto = getValidTerminologyMetadata();
        dto.setLabel(Map.of());
        args.add(new DataWithError("should-have-value", dto));

        // organization
        dto = getValidTerminologyMetadata();
        dto.setOrganizations(Set.of());
        args.add(new DataWithError("should-have-value", dto));

        dto = getValidTerminologyMetadata();
        var orgId = UUID.randomUUID();
        dto.setOrganizations(Set.of(orgId));
        args.add(new DataWithError("does-not-exist." + orgId, dto));

        // groups
        dto = getValidTerminologyMetadata();
        dto.setGroups(Set.of());
        args.add(new DataWithError("should-have-value", dto));

        dto = getValidTerminologyMetadata();
        dto.setGroups(Set.of("P999"));
        args.add(new DataWithError("does-not-exist.P999", dto));

        // status
        dto = getValidTerminologyMetadata();
        dto.setStatus(null);
        args.add(new DataWithError("should-have-value", dto));

        // languages
        dto = getValidTerminologyMetadata();
        dto.setLanguages(Set.of());
        args.add(new DataWithError("should-have-value", dto));

        dto = getValidTerminologyMetadata();
        dto.setLanguages(Set.of("invalid"));
        args.add(new DataWithError("does-not-match-rfc-4646", dto));

        // graph type
        dto = getValidTerminologyMetadata();
        dto.setGraphType(GraphType.LIBRARY);
        args.add(new DataWithError("invalid-value", dto));

        dto = getValidTerminologyMetadata();
        dto.setGraphType(null);
        args.add(new DataWithError("should-have-value", dto));

        return args;
    }

    private static TerminologyDTO getValidTerminologyMetadata() {
        var terminologyDTO = new TerminologyDTO();

        terminologyDTO.setOrganizations(Set.of(ORGANIZATION_ID));
        terminologyDTO.setGraphType(GraphType.TERMINOLOGICAL_VOCABULARY);
        terminologyDTO.setContact("Contact");
        terminologyDTO.setGroups(Set.of("P10"));
        terminologyDTO.setLabel(Map.of("en", "Test terminology"));
        terminologyDTO.setDescription(Map.of("en", "Test description"));
        terminologyDTO.setLanguages(Set.of("en"));
        terminologyDTO.setPrefix("test");
        terminologyDTO.setStatus(Status.VALID);
        return terminologyDTO;
    }

    record DataWithError(String errorMessage, MetaDataDTO data) {}
}
