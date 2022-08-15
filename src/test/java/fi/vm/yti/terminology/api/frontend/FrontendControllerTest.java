package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.ExceptionHandlerAdvice;
import fi.vm.yti.terminology.api.model.termed.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.*;
import java.util.stream.Stream;

import static fi.vm.yti.terminology.api.validation.ValidationConstants.*;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(controllers = FrontendController.class)
public class FrontendControllerTest {

    public static final String TEMPLATE_GRAPH_ID = "61cf6bde-46e6-40bb-b465-9b2c66bf4ad8";

    private static final String TEST_NODE_ID = "75ad9343-c3f7-417f-99e4-7aff07f5daf6";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private FrontendTermedService termedService;

    @MockBean
    private FrontendElasticSearchService elasticSearchService;

    @MockBean
    private FrontendGroupManagementService groupManagementService;

    @MockBean
    private AuthenticatedUserProvider userProvider;

    @Autowired
    private FrontendController frontendController;

    @MockBean
    private ObjectMapper objectMapper;

    private LocalValidatorFactoryBean localValidatorFactory;

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(this.frontendController)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();
    }

    @Test
    public void shouldValidateAndCreate() throws Exception {
        var vocabularyNode = this.constructVocabularyNode();

        this.mvc
                .perform(post("/api/v1/frontend/validatedVocabulary")
                        .param("prefix", "test1")
                        .param("templateGraphId", TEMPLATE_GRAPH_ID)
                        .contentType("application/json")
                        .content(convertObjectToJsonString(vocabularyNode)))
                // .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().string(this.uuidMatcher));

        verify(this.termedService)
                .createVocabulary(
                        any(UUID.class),
                        any(String.class),
                        any(GenericNode.class),
                        any(UUID.class),
                        eq(true));
        verifyNoMoreInteractions(this.termedService);
    }

    @ParameterizedTest
    @MethodSource("providePrefixes")
    public void shouldValidatePrefix(String prefix, boolean shouldSucceed) throws Exception {

        // templateGraph UUID, predefined in database

        var vocabularyNode = this.constructVocabularyNode();

        var request = this.mvc
                .perform(post("/api/v1/frontend/validatedVocabulary")
                        .param("prefix", prefix)
                        .param("templateGraphId", TEMPLATE_GRAPH_ID)
                        .contentType("application/json")
                        .content(convertObjectToJsonString(vocabularyNode)));

        if (shouldSucceed) {
            request
                    // .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(content().string(this.uuidMatcher));

            verify(this.termedService)
                    .createVocabulary(
                            any(UUID.class),
                            any(String.class),
                            any(GenericNode.class),
                            any(UUID.class),
                            eq(true));

        } else {
            request
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.message").value(
                            "Object validation failed"))
                    .andExpect(jsonPath("$.details").exists());
        }

        verifyNoMoreInteractions(this.termedService);
    }

    private static Stream<Arguments> providePrefixes() {
        return Stream.of(
                Arguments.of("tes", true),
                Arguments.of("te", false),
                Arguments.of("", false)
        );
    }

    @ParameterizedTest
    @MethodSource("provideVocabularyNodesWithMissingData")
    public void shouldFailOnMissingData(GenericNode vocabularyNode) throws Exception {
        this.mvc
                .perform(post("/api/v1/frontend/vocabulary/validate")
                        .param("prefix", "test1")
                        .param("templateGraphId", TEMPLATE_GRAPH_ID)
                        .contentType("application/json")
                        .content(convertObjectToJsonString(vocabularyNode)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").value("Object validation failed"))
                .andExpect(jsonPath("$.details").exists());

        verify(this.termedService, times(0))
                .createVocabulary(
                        any(UUID.class),
                        any(String.class),
                        any(GenericNode.class),
                        any(UUID.class),
                        eq(true));
        verifyNoMoreInteractions(this.termedService);
    }

    @ParameterizedTest
    @MethodSource("provideVocabularyNodesWithTooLongData")
    public void shouldFailOnTooLongData(GenericNode vocabularyNode) throws Exception {
        this.mvc
                .perform(post("/api/v1/frontend/vocabulary/validate")
                        .param("prefix", "test1")
                        .param("templateGraphId", TEMPLATE_GRAPH_ID)
                        .contentType("application/json")
                        .content(convertObjectToJsonString(vocabularyNode)))
                .andDo(log())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").value("Object validation failed"))
                .andExpect(jsonPath("$.details").exists());

        verify(this.termedService, times(0))
                .createVocabulary(
                        any(UUID.class),
                        any(String.class),
                        any(GenericNode.class),
                        any(UUID.class),
                        eq(true));
        verifyNoMoreInteractions(this.termedService);
    }

    @Test
    public void shouldValidateOnly() throws Exception {

        // templateGraph UUID, predefined in database


        var vocabularyNode = constructVocabularyNode();

        this.mvc
                .perform(post("/api/v1/frontend/vocabulary/validate")
                        .param("prefix", "test1")
                        .param("templateGraphId", TEMPLATE_GRAPH_ID)
                        .contentType("application/json")
                        .content(convertObjectToJsonString(vocabularyNode)))
                // .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().string("OK"));

        verifyNoMoreInteractions(this.termedService);
    }

    @Test
    public void shouldFailOnLanguageMismatch() throws Exception {
        var properties = constructProperties();

        properties.remove("language");
        properties.put("language", Arrays.asList(
                new Attribute("", "en"),
                new Attribute("", "fi")
        ));
        var vocabularyNode = constructVocabularyNode(
                properties,
                constructReferences(),
                constructReferrers());

        this.mvc
                .perform(post("/api/v1/frontend/vocabulary/validate")
                        .param("prefix", "test1")
                        .param("templateGraphId", TEMPLATE_GRAPH_ID)
                        .contentType("application/json")
                        .content(convertObjectToJsonString(vocabularyNode)))
                //.andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").value("Object validation failed"))
                .andExpect(jsonPath("$.details").exists());

        verifyNoMoreInteractions(this.termedService);
    }

    private static Stream<Arguments> provideVocabularyNodesWithMissingData() {
        var args = new ArrayList<GenericNode>();

        // without a prefLabel
        var properties = constructProperties();
        properties.remove("prefLabel");
        args.add(constructVocabularyNode(
                properties, constructReferences(), constructReferrers()));

        // with empty prefLabel
        properties = constructProperties();
        properties.remove("prefLabel");
        properties.put("prefLabel",
                singletonList(new Attribute("en", "")));
        args.add(constructVocabularyNode(
                properties, constructReferences(), constructReferrers()));

        // without terminologyType
        properties = constructProperties();
        properties.remove("terminologyType");
        args.add(constructVocabularyNode(
                properties, constructReferences(), constructReferrers()));

        // without a status
        properties = constructProperties();
        properties.remove("status");
        args.add(constructVocabularyNode(
                properties, constructReferences(), constructReferrers()));

        // without contributor
        var references = constructReferences();
        references.remove("contributor");
        references.put("contributor", new ArrayList<>());
        args.add(constructVocabularyNode(
                constructProperties(), references, constructReferrers()));

        // without group
        references = constructReferences();
        references.remove("inGroup");
        references.put("inGroup", new ArrayList<>());
        args.add(constructVocabularyNode(
                constructProperties(), references, constructReferrers()));

        return args.stream().map(Arguments::of);
    }

    private static Stream<Arguments> provideVocabularyNodesWithTooLongData(){
        var args = new ArrayList<GenericNode>();

        final var textFieldMaxPlus = TEXT_FIELD_MAX_LENGTH + 20;
        final var textAreaMaxPlus = TEXT_AREA_MAX_LENGTH + 20;
        final var emailMaxPlus = EMAIL_MAX_LENGTH + 20;

        var properties = constructProperties();
        properties.replace("prefLabel", List.of(new Attribute("en", RandomStringUtils.random(textFieldMaxPlus))));
        args.add(constructVocabularyNode(
                properties, constructReferences(), constructReferrers()));

        properties = constructProperties();
        properties.replace("description", List.of(new Attribute("en", RandomStringUtils.random(textAreaMaxPlus))));
        args.add(constructVocabularyNode(
                properties, constructReferences(), constructReferrers()));

        properties = constructProperties();
        properties.replace("contact", List.of(new Attribute("en", RandomStringUtils.random(emailMaxPlus))));
        args.add(constructVocabularyNode(
                properties, constructReferences(), constructReferrers()));

        return args.stream().map(Arguments::of);
    }

    private static HashMap<String, List<Attribute>> constructProperties() {
        // prepare vocabularyNode for creating a new vocabulary
        var properties = new HashMap<String, List<Attribute>>();
        properties.put(
                "prefLabel",
                Arrays.asList(
                        new Attribute("en", "test label")
                ));
        properties.put("description", new ArrayList());
        properties.put(
                "language",
                singletonList(new Attribute("", "en")));
        properties.put(
                "terminologyType",
                singletonList(new Attribute("", "TERMINOLOGICAL_VOCABULARY")));
        properties.put(
                "status",
                singletonList(new Attribute("", "DRAFT")));
        properties.put(
                "priority",
                singletonList(new Attribute("", "")));
        properties.put(
                "contact",
                singletonList(new Attribute("", "")));
        properties.put(
                "origin",
                singletonList(new Attribute("", "")));

        return properties;
    }

    private static HashMap<String, List<Attribute>> constructPropertiesInvalidLength() {
        // prepare vocabularyNode for creating a new vocabulary
        var properties = new HashMap<String, List<Attribute>>();
        properties.put(
                "prefLabel",
                Arrays.asList(
                        new Attribute("en", "test label")
                ));
        properties.put("description", new ArrayList());
        properties.put(
                "language",
                singletonList(new Attribute("", "en")));
        properties.put(
                "terminologyType",
                singletonList(new Attribute("", "TERMINOLOGICAL_VOCABULARY")));
        properties.put(
                "status",
                singletonList(new Attribute("", "DRAFT")));
        properties.put(
                "priority",
                singletonList(new Attribute("", "")));
        properties.put(
                "contact",
                singletonList(new Attribute("", "")));
        properties.put(
                "origin",
                singletonList(new Attribute("", "")));

        return properties;
    }

    private static HashMap<String, List<Identifier>> constructReferences() {
        var references = new HashMap<String, List<Identifier>>();

        var org1 = new Identifier(
                UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63"),
                new TypeId(
                        NodeType.Organization,
                        new GraphId(UUID.fromString("228cce1e-8360-4039-a3f7-725df5643354"))));
        references.put("contributor", Arrays.asList(org1));

        var group1 = new Identifier(
                UUID.fromString("9ba19f8d-d688-39cc-b620-ebb7875e6e9b"),
                new TypeId(
                        NodeType.Group,
                        new GraphId(UUID.fromString("7f4cb68f-31f6-4bf9-b699-9d72dd110c4c"))));
        references.put("inGroup", Arrays.asList(group1));

        return references;
    }

    private static HashMap<String, List<Identifier>> constructReferrers() {
        var referrers = new HashMap<String, List<Identifier>>();
        return referrers;
    }

    private static GenericNode constructVocabularyNode() {
        return constructVocabularyNode(
                constructProperties(),
                constructReferences(),
                constructReferrers());
    }

    private static GenericNode constructVocabularyNode(
            Map<String, List<Attribute>> properties,
            Map<String, List<Identifier>> references,
            Map<String, List<Identifier>> referrers) {

        // templateGraph UUID, predefined in database


        var vocabularyNode = new GenericNode(
                UUID.fromString(TEST_NODE_ID),
                null,
                null,
                40L,
                "creator",
                new Date(),             // createdDate
                "modifier",
                new Date(),             // lastModifiedDate

                // type
                new TypeId(
                        NodeType.TerminologicalVocabulary,
                        new GraphId(UUID.fromString(TEMPLATE_GRAPH_ID))),

                properties,             // properties
                references,             // references
                referrers               // referrers
        );

        return vocabularyNode;
    }

    private String convertObjectToJsonString(GenericNode node) throws JsonProcessingException {
        var mapper = new ObjectMapper();
        return mapper.writeValueAsString(node);
    }

    private final Matcher<String> uuidMatcher = Matchers.matchesRegex(
            "^\"?[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\"?$");
}
