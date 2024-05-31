package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.common.repository.CommonRepository;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionInfoDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptInfoDTO;
import fi.vm.yti.terminology.api.v2.exception.TerminologyExceptionHandlerAdvice;
import fi.vm.yti.terminology.api.v2.service.ConceptCollectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.util.Set;

import static fi.vm.yti.terminology.api.v2.TestUtils.asJsonString;
import static fi.vm.yti.terminology.api.v2.TestUtils.getConceptCollectionData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        var conceptCollectionDTO = getConceptCollectionData();

        URI conceptCollectionURI = new URI(
                "https://iri.suomi.fi/terminology/test/collection-1");
        when(conceptCollectionService.create(
                eq("test"),
                any(ConceptCollectionDTO.class))).thenReturn(conceptCollectionURI);

        this.mvc
                .perform(post("/v2/collection/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(conceptCollectionDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        conceptCollectionURI.toString()));

        verify(conceptCollectionService).create(
                eq("test"),
                any(ConceptCollectionDTO.class));
        verify(conceptCollectionService).create(
                eq("test"),
                argThat(dto -> {
                    if (!dto.getIdentifier().equals("collection-1")) {
                        return false;
                    }
                    if (!dto.getMembers().containsAll(
                            Set.of("concept-1", "concept-2"))) {
                        return false;
                    }
                    return true;
                }));
        verifyNoMoreInteractions(this.conceptCollectionService);
    }

    @Test
    void shouldValidateAndUpdate() throws Exception {
        var conceptCollectionDTO = getConceptCollectionData();
        conceptCollectionDTO.setIdentifier("collection-1");

        this.mvc
                .perform(put("/v2/collection/test/collection-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(conceptCollectionDTO)))
                .andExpect(status().isNoContent());
        verify(conceptCollectionService).update(
                eq("test"),
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
}
