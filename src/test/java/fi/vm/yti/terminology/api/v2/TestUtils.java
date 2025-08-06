package fi.vm.yti.terminology.api.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.common.dto.*;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.properties.DCAP;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.LocalizedValueDTO;
import fi.vm.yti.terminology.api.v2.dto.TermDTO;
import fi.vm.yti.terminology.api.v2.enums.TermConjugation;
import fi.vm.yti.terminology.api.v2.enums.TermEquivalency;
import fi.vm.yti.terminology.api.v2.enums.TermFamily;
import fi.vm.yti.terminology.api.v2.enums.WordClass;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.DCTerms;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestUtils {

    public static ModelWrapper getModelFromFile(String filepath, String graphURI) {
        var model = ModelFactory.createDefaultModel();
        var stream = TestUtils.class.getResourceAsStream(filepath);
        assertNotNull(stream);
        RDFDataMgr.read(model, stream, RDFLanguages.TURTLE);
        return new ModelWrapper(model, graphURI);
    }

    public static ModelWrapper getDefaultModel(String prefix) {
        var graphURI = TerminologyURI.Factory.createTerminologyURI(prefix).getGraphURI();

        var m = ModelFactory.createDefaultModel();
        m.createResource(graphURI)
                .addProperty(DCTerms.language, "fi")
                .addProperty(DCTerms.language, "sv")
                .addProperty(DCTerms.language, "en")
                .addProperty(DCAP.preferredXMLNamespacePrefix, prefix);
        return new ModelWrapper(m, graphURI);
    }

    public static final YtiUser mockUser = new YtiUser("test@localhost",
            "test",
            "tester",
            UUID.randomUUID(),
            false,
            false,
            LocalDateTime.of(2001, 1, 1, 0,0),
            LocalDateTime.of(2001, 1, 1, 0,0),
            new HashMap<>(Map.of(UUID.randomUUID(), Set.of(Role.TERMINOLOGY_EDITOR))),
            "",
            "");

    public static final UUID organizationId = UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63");

    public static final List<ServiceCategoryDTO> categoryDTOs = List.of(
            new ServiceCategoryDTO(
                    "http://urn.fi/URN:NBN:fi:au:ptvl:v1096",
                    Map.of("en", "Sample category P10"),
                    "P10"
            ),
            new ServiceCategoryDTO(
                    "http://urn.fi/URN:NBN:fi:au:ptvl:v1097",
                    Map.of("en", "Sample category P11"),
                    "P11"
            ));

    public static final List<OrganizationDTO> organizationDTOs = List.of(new OrganizationDTO(
            organizationId.toString(),
            Map.of("en", "Test organization"),
            null)
    );

    public static Consumer<ResourceCommonInfoDTO> mapUser = (var dto) -> {
        var creator = new UserDTO("123");
        var modifier = new UserDTO("123");
        creator.setName("creator fake-user");
        modifier.setName("modifier fake-user");
        dto.setCreator(creator);
        dto.setModifier(modifier);
    };

    public static String asJsonString(Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

    public static ConceptDTO getConceptData() {
        var dto = new ConceptDTO();
        dto.setConceptClass("conceptClass");
        dto.setChangeNote("change");
        dto.setDefinition(Map.of("en", "definition"));
        dto.setStatus(Status.VALID);
        dto.setExamples(List.of(new LocalizedValueDTO("en", "example")));
        dto.setIdentifier("concept-1");
        dto.setNotes(List.of(new LocalizedValueDTO("en", "note")));
        dto.setEditorialNotes(List.of("editorial"));
        dto.setHistoryNote("history");
        dto.setSubjectArea("subject area");
        dto.setSources(List.of("source"));

        var link = new LinkDTO();
        link.setName(Map.of("en", "link"));
        link.setDescription(Map.of("en", "link description"));
        link.setUri("https://dvv.fi");
        dto.setLinks(List.of(link));

        Function<String, String> refURI =
                localName -> TerminologyURI.Factory.createConceptURI("test", localName).getResourceURI();

        var broader = new LinkedHashSet<String>();
        broader.add(refURI.apply("broader-1"));
        broader.add(refURI.apply("broader-2"));
        broader.add(refURI.apply("broader-3"));
        dto.setBroader(broader);
        dto.setNarrower(Set.of(refURI.apply("narrower")));
        dto.setIsPartOf(Set.of(refURI.apply("isPartOf")));
        dto.setHasPart(Set.of(refURI.apply("hasPart")));
        dto.setRelated(Set.of(refURI.apply("related")));
        dto.setBroadMatch(Set.of(refURI.apply("broadMatch")));
        dto.setNarrowMatch(Set.of(refURI.apply("narrowMatch")));
        dto.setExactMatch(Set.of(refURI.apply("exactMatch")));
        dto.setCloseMatch(Set.of(refURI.apply("closeMatch")));
        dto.setRelatedMatch(Set.of(refURI.apply("relatedMatch")));

        var recommendedTerms = new ArrayList<TermDTO>();
        recommendedTerms.add(getTermDTO());
        dto.setRecommendedTerms(recommendedTerms);

        var synonym1 = new TermDTO();
        var synonym2 = new TermDTO();
        synonym1.setLanguage("en");
        synonym1.setLabel("Synonym 1");
        synonym1.setStatus(Status.DRAFT);
        synonym2.setLanguage("en");
        synonym2.setLabel("Synonym 2");
        synonym2.setStatus(Status.DRAFT);
        dto.setSynonyms(List.of(synonym1, synonym2));

        return dto;
    }

    public static ConceptCollectionDTO getConceptCollectionData(String prefix) {
        var dto = new ConceptCollectionDTO();
        dto.setIdentifier("collection-1");
        dto.setLabel(Map.of("en", "collection label"));
        dto.setDescription(Map.of("en", "collection description"));
        dto.addMember(TerminologyURI.Factory
                .createConceptURI(prefix, "concept-1")
                .getResourceURI());
        dto.addMember(TerminologyURI.Factory
                .createConceptURI(prefix, "concept-2")
                .getResourceURI());

        return dto;
    }

    public static TermDTO getTermDTO() {
        var term = new TermDTO();
        term.setHomographNumber(1);
        term.setLabel("term label");
        term.setChangeNote("change");
        term.setHistoryNote("history");
        term.setLanguage("en");
        term.setStatus(Status.VALID);
        term.setScope("scope");
        term.setTermInfo("info");
        term.setTermFamily(TermFamily.NEUTER);
        term.setTermConjugation(TermConjugation.SINGULAR);
        term.setWordClass(WordClass.VERB);
        term.setTermStyle("style");
        term.setTermEquivalency(TermEquivalency.BROADER);
        return term;
    }

    public static String getJsonString(String file) throws Exception {
        return new String(TestUtils.class
                .getResourceAsStream(file).readAllBytes(), StandardCharsets.UTF_8);
    }

    public static void writeModel(Model model) {
        RDFDataMgr.write(System.out, model, Lang.TURTLE);
    }
}
