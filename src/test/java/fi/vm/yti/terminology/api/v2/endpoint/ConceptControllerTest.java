package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.common.repository.CommonRepository;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.validator.ValidationConstants;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptInfoDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptReferenceDTO;
import fi.vm.yti.terminology.api.v2.dto.LocalizedValueDTO;
import fi.vm.yti.terminology.api.v2.enums.ReferenceType;
import fi.vm.yti.terminology.api.v2.exception.TerminologyExceptionHandlerAdvice;
import fi.vm.yti.terminology.api.v2.service.ConceptService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fi.vm.yti.terminology.api.v2.TestUtils.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ConceptController.class)
@ActiveProfiles("test")
class ConceptControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CommonRepository commonRepository;

    @MockBean
    private GroupManagementService groupManagementService;

    @MockBean
    private ConceptService conceptService;

    @Autowired
    ConceptController conceptController;

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.conceptController)
                .setControllerAdvice(new TerminologyExceptionHandlerAdvice())
                .build();
    }

    @Test
    void shouldValidateAndCreate() throws Exception {
        var conceptDTO = getConceptData();

        URI conceptURI = new URI("https://iri.suomi.fi/terminology/test/concept-1");
        when(conceptService.create(eq("test"), any(ConceptDTO.class))).thenReturn(conceptURI);

        this.mvc
                .perform(post("/v2/concept/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(conceptDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", conceptURI.toString()));

        verify(conceptService).create(eq("test"), any(ConceptDTO.class));
        verifyNoMoreInteractions(this.conceptService);
    }

    @Test
    void shouldValidateAndUpdate() throws Exception {
        var conceptDTO = getConceptData();
        conceptDTO.setIdentifier(null);

        this.mvc
                .perform(put("/v2/concept/test/concept-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(conceptDTO)))
                .andExpect(status().isNoContent());

        verify(conceptService).update(eq("test"), eq("concept-1"), any(ConceptDTO.class));
        verifyNoMoreInteractions(this.conceptService);
    }

    @Test
    void shouldGetConcept() throws Exception {

        when(conceptService.get("test", "concept-1")).thenReturn(new ConceptInfoDTO());

        this.mvc
                .perform(get("/v2/concept/test/concept-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteConcept() throws Exception {
        this.mvc
                .perform(delete("/v2/concept/test/concept-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(conceptService).delete("test", "concept-1");
    }

    @Test
    void shouldCheckConceptExists() throws Exception {
        this.mvc
                .perform(get("/v2/concept/test/concept-1/exists")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(conceptService).exists("test", "concept-1");
    }

    @ParameterizedTest
    @MethodSource("provideInvalidConceptCreateData")
    void shouldInvalidateConceptOnCreation(ConceptWithError data) throws Exception {

        this.mvc
                .perform(post("/v2/concept/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(data.dto)))
                .andExpect(content().string(containsString(data.error())))
                .andExpect(status().isBadRequest());

        verifyNoMoreInteractions(this.conceptService);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidConceptUpdateData")
    void shouldInvalidateConceptOnUpdate(ConceptWithError data) throws Exception {
        this.mvc
                .perform(put("/v2/concept/test/concept-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(data.dto)))
                .andExpect(content().string(containsString(data.error())))
                .andExpect(status().isBadRequest());

        verifyNoMoreInteractions(this.conceptService);
    }

    public static ArrayList<ConceptWithError> provideInvalidConceptCreateData() {
        var args = new ArrayList<ConceptWithError>();

        var dto = getConceptData();
        dto.setIdentifier(null);
        args.add(new ConceptWithError("should-have-value", dto));

        args.addAll(provideInvalidConceptData());

        return args;
    }

    public static ArrayList<ConceptWithError> provideInvalidConceptUpdateData() {
        var args = new ArrayList<ConceptWithError>();

        var dto = getConceptData();
        args.add(new ConceptWithError("not-allowed-update", dto));

        provideInvalidConceptData().forEach(data -> {
            data.dto.setIdentifier(null);
            args.add(data);
        });
        return args;
    }

    public static ArrayList<ConceptWithError> provideInvalidConceptData() {
        var args = new ArrayList<ConceptWithError>();

        var longTextArea = RandomStringUtils.randomAlphabetic(ValidationConstants.TEXT_AREA_MAX_LENGTH + 1);
        var longTextField = RandomStringUtils.randomAlphabetic(ValidationConstants.TEXT_FIELD_MAX_LENGTH + 1);

        // check concept data
        var dto = getConceptData();
        dto.setNotes(List.of(new LocalizedValueDTO("en", longTextArea)));
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        dto.setExamples(List.of(new LocalizedValueDTO("en", longTextArea)));
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        dto.setSources(List.of(longTextArea));
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        dto.setEditorialNotes(List.of(longTextArea));
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        dto.setHistoryNote(longTextArea);
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        dto.setChangeNote(longTextArea);
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        dto.setConceptClass(longTextField);
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        dto.setSubjectArea(Map.of("en", longTextField));
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        var ref = new ConceptReferenceDTO();
        ref.setReferenceType(ReferenceType.BROADER);
        dto.setReferences(Set.of(ref));
        args.add(new ConceptWithError("should-have-value", dto));

        // check term data

        dto = getConceptData();
        var term = getTermDTO();
        term.setLanguage("");
        dto.setTerms(Set.of(term));
        args.add(new ConceptWithError("should-have-value", dto));

        dto = getConceptData();
        term = getTermDTO();
        term.setLabel(longTextField);
        dto.setTerms(Set.of(term));
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        term = getTermDTO();
        term.setLabel("");
        dto.setTerms(Set.of(term));
        args.add(new ConceptWithError("should-have-value", dto));

        dto = getConceptData();
        term = getTermDTO();
        term.setTermStyle(longTextField);
        dto.setTerms(Set.of(term));
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        term = getTermDTO();
        term.setScope(longTextArea);
        dto.setTerms(Set.of(term));
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        term = getTermDTO();
        term.setTermInfo(longTextArea);
        dto.setTerms(Set.of(term));
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        term = getTermDTO();
        term.setHistoryNote(longTextArea);
        dto.setTerms(Set.of(term));
        args.add(new ConceptWithError("value-over-character-limit", dto));

        dto = getConceptData();
        term = getTermDTO();
        term.setChangeNote(longTextArea);
        dto.setTerms(Set.of(term));
        args.add(new ConceptWithError("value-over-character-limit", dto));

        return args;
    }

    record ConceptWithError(String error, ConceptDTO dto) {}
}
