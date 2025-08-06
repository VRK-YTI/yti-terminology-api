package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.common.repository.CommonRepository;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.validator.ValidationConstants;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionInfoDTO;
import fi.vm.yti.terminology.api.v2.exception.TerminologyExceptionHandlerAdvice;
import fi.vm.yti.terminology.api.v2.service.ConceptCollectionService;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static fi.vm.yti.terminology.api.v2.TestUtils.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ConceptCollectionController.class)
@ActiveProfiles("test")
public class ConceptCollectionControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private CommonRepository commonRepository;

    @MockBean
    private GroupManagementService groupManagementService;

    @MockBean
    private ConceptCollectionService conceptCollectionService;

    @Autowired
    ConceptCollectionController conceptCollectionController;

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.conceptCollectionController)
                .setControllerAdvice(new TerminologyExceptionHandlerAdvice())
                .build();
    }

    @Test
    void shouldValidateAndCreate() throws Exception {
        var modelPrefix = "test";
        var conceptCollectionDTO = getConceptCollectionData(modelPrefix);

        URI conceptCollectionURI = new URI(String.format(
                "https://iri.suomi.fi/terminology/%s/collection-1",
                modelPrefix));
        when(conceptCollectionService.create(
                eq(modelPrefix),
                any(ConceptCollectionDTO.class))).thenReturn(conceptCollectionURI);

        this.mvc
                .perform(post(String.format("/v2/collection/%s", modelPrefix))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(conceptCollectionDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        conceptCollectionURI.toString()));

        verify(conceptCollectionService).create(
                eq(modelPrefix),
                any(ConceptCollectionDTO.class));
        verify(conceptCollectionService).create(
                eq(modelPrefix),
                argThat(dto -> {
                    if (!dto.getIdentifier().equals("collection-1")) {
                        return false;
                    }
                    var expectedConceptUris = Set.of(
                            TerminologyURI.Factory
                                    .createConceptURI(modelPrefix, "concept-1")
                                    .getResourceURI(),
                            TerminologyURI.Factory
                                    .createConceptURI(modelPrefix, "concept-2")
                                    .getResourceURI()
                    );
                    if (!dto.getMembers().containsAll(expectedConceptUris)) {
                        return false;
                    }
                    return true;
                }));
        verifyNoMoreInteractions(this.conceptCollectionService);
    }

    @Test
    void shouldValidateAndUpdate() throws Exception {
        var modelPrefix = "test";
        var conceptCollectionDTO = getConceptCollectionData(modelPrefix);
        conceptCollectionDTO.setIdentifier(null);

        this.mvc
                .perform(put(String.format("/v2/collection/%s/collection-1", modelPrefix))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(conceptCollectionDTO)))
                .andExpect(status().isNoContent());
        verify(conceptCollectionService).update(
                eq(modelPrefix),
                eq("collection-1"),
                any(ConceptCollectionDTO.class));
        verifyNoMoreInteractions(this.conceptCollectionService);
    }

    @Test
    void shouldGetConceptCollection() throws Exception {
        when(conceptCollectionService.get(
                "test",
                "collection-1"))
                .thenReturn(new ConceptCollectionInfoDTO());

        this.mvc
                .perform(get("/v2/collection/test/collection-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteConceptCollection() throws Exception {
        this.mvc
                .perform(delete("/v2/collection/test/collection-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(conceptCollectionService).delete(
                "test",
                "collection-1");
    }

    @Test
    void shouldCheckConceptCollectionExists() throws Exception {
        this.mvc
                .perform(get("/v2/collection/test/collection-1/exists")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(conceptCollectionService).exists("test", "collection-1");
    }

    @ParameterizedTest
    @MethodSource("provideInvalidConceptCollectionCreateData")
    void shouldInvalidateConceptCollectionOnCreation(ConceptCollectionWithError data) throws Exception {

        this.mvc
                .perform(post("/v2/collection/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(data.dto)))
                .andExpect(content().string(containsString(data.error())))
                .andExpect(status().isBadRequest());

        verifyNoMoreInteractions(this.conceptCollectionService);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidConceptCollectionUpdateData")
    void shouldInvalidateConceptCollectionOnUpdate(ConceptCollectionWithError data) throws Exception {
        this.mvc
                .perform(put("/v2/collection/test/collection-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(data.dto)))
                .andExpect(content().string(containsString(data.error())))
                .andExpect(status().isBadRequest());

        verifyNoMoreInteractions(this.conceptCollectionService);
    }

    public static ArrayList<ConceptCollectionWithError> provideInvalidConceptCollectionCreateData() {
        var args = new ArrayList<ConceptCollectionWithError>();

        var dto = getConceptCollectionData("test");
        dto.setIdentifier(null);
        args.add(new ConceptCollectionWithError("should-have-value", dto));

        args.addAll(provideInvalidConceptCollectionData());

        return args;
    }

    public static ArrayList<ConceptCollectionWithError> provideInvalidConceptCollectionUpdateData() {
        var args = new ArrayList<ConceptCollectionWithError>();

        var dto = getConceptCollectionData("test");
        args.add(new ConceptCollectionWithError("not-allowed-update", dto));

        provideInvalidConceptCollectionData().forEach(data -> {
            data.dto.setIdentifier(null);
            args.add(data);
        });
        return args;
    }

    public static ArrayList<ConceptCollectionWithError> provideInvalidConceptCollectionData() {
        var args = new ArrayList<ConceptCollectionWithError>();

        var longTextField = RandomStringUtils.randomAlphabetic(ValidationConstants.TEXT_FIELD_MAX_LENGTH + 1);
        var longTextArea = RandomStringUtils.randomAlphabetic(ValidationConstants.TEXT_AREA_MAX_LENGTH + 1);

        var dto = getConceptCollectionData("test");
        dto.setLabel(Map.of("en", longTextField));
        args.add(new ConceptCollectionWithError("value-over-character-limit", dto));

        dto = getConceptCollectionData("test");
        dto.setDescription(Map.of("en", longTextArea));
        args.add(new ConceptCollectionWithError("value-over-character-limit", dto));

        return args;
    }

    record ConceptCollectionWithError(String error, ConceptCollectionDTO dto) {}
}
