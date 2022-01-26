package fi.vm.yti.terminology.api.frontend;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.config.JsonConfig;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchRequest;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchResponse;
import fi.vm.yti.terminology.api.util.RestHighLevelClientWrapper;
import fi.vm.yti.terminology.elasticsearch.EsUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@Import({ JsonConfig.class, FrontendElasticSearchService.class })
@ExtendWith(SpringExtension.class) // TODO: read this https://rieckpil.de/what-the-heck-is-the-springextension-used-for/
@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "search.index.name=testIndex"
})
public class FrontendElasticSearchServiceTest {

    // provided by org.springframework.context.support.GenericApplicationContext
    @Autowired
    ResourceLoader resourceLoader;

    @MockBean()
    RestHighLevelClientWrapper esClient;

    @MockBean
    AuthenticatedUserProvider userProvider;

    // not a mock, using the same Bean as the application from JsonConfig
    @Autowired
    ObjectMapper objectMapper;

    // service under test
    @Autowired
    FrontendElasticSearchService service;

    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    void setup() {
        // make sure DI is functioning
        assertNotNull(service);
        assertNotNull(esClient);
        assertNotNull(objectMapper);

        this.initLogWatcher();
    }

    @AfterEach
    void cleanup() {
        this.cleanLogWatcher();
    }

    private void cleanLogWatcher() {
        var logger = (Logger) LoggerFactory.getLogger(FrontendElasticSearchService.class);
        logger.detachAndStopAllAppenders();
    }

    private void initLogWatcher() {
        this.logWatcher = new ListAppender<>();
        this.logWatcher.start();
        // cast from facade (SLF4J) to implementation class (logback)
        var logger = (Logger) LoggerFactory.getLogger(FrontendElasticSearchService.class);
        logger.addAppender(this.logWatcher);
    }

    private void assertNoLogErrors() {
        var errors = logWatcher.list.stream()
                .filter(x -> "ERROR".equals(x.getLevel().toString()))
                .collect(Collectors.toList());
        assertEquals(0, errors.size());
    }

    private YtiUser createMockUser(boolean superUser, Map<UUID, Set<Role>> roles) {
        return new YtiUser(
                "test@test.invalid",
                "firstname",
                "lastname",
                UUID.fromString("384c982c-7254-43df-ab7c-5037b6fb71c0"),
                superUser,
                false,
                LocalDateTime.of(2005, 4, 2, 1, 10),
                LocalDateTime.of(2006, 4, 2, 1, 10),
                roles,
                "",
                "");
    }

    private YtiUser createMockUser(boolean superUser) {
        return this.createMockUser(superUser, new HashMap<>());
    }

    private boolean isVocabularyQuery(SearchRequest arg)
    {
        if (arg.indices().length != 1 || !"vocabularies".equals(arg.indices()[0])) {
            return false;
        }
        return true;
    }

    private boolean isConceptQuery(SearchRequest arg)
    {
        if (arg.indices().length != 1 || !"concepts".equals(arg.indices()[0])) {
            return false;
        }
        return true;
    }

    @Test
    public void searchWithoutConcepts() throws Exception {
        // convert JSON sample data to SearchResponse for returning as mock
        // values from esClient searches
        var vocabulariesResponse = EsUtils.getMockResponse(
                "/es/response/vocabulary_response.json");
        assertNotNull(vocabulariesResponse);

        // getUser is being used to check for superUser and organizations
        doReturn(this.createMockUser(false))
                .when(this.userProvider)
                .getUser();

        // Mock the same esClient call with a specific argument
        doReturn(vocabulariesResponse)
                .when(this.esClient)
                .search(argThat(i -> isVocabularyQuery(i)), any());

        // Actual query
        var request = new TerminologySearchRequest();
        request.setQuery("test");
        request.setSearchConcepts(false); // false == no additional concept search
        var response = service.searchTerminology(request);
        assertNotNull(response);

        // Make sure the mocks were called as expected
        verify(this.userProvider, times(2))
                .getUser();

        ArgumentCaptor<SearchRequest> srCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(this.esClient, times(1))
                .search(srCaptor.capture(), any(RequestOptions.class));

        // Check that esClient.search was called with expected kinds of arguments
        var args = srCaptor.getAllValues();
        assertEquals(1, args.size());

        var vocabulariesRequest = args.get(0);
        assertEquals("vocabularies",
                String.join(",", vocabulariesRequest.indices()));

        verifyNoMoreInteractions(esClient);
        assertNoLogErrors();
    }

    @Test
    public void searchWithConcepts() throws Exception {
        // convert JSON sample data to SearchResponse for returning as mock
        // values from esClient searches
        var conceptsResponse = EsUtils.getMockResponse(
                "/es/response/concept_response.json");
        assertNotNull(conceptsResponse);
        var vocabulariesResponse = EsUtils.getMockResponse(
                "/es/response/vocabulary_response.json");
        assertNotNull(vocabulariesResponse);

        // getUser is being used to check for superUser and organizations
        doReturn(this.createMockUser(false))
                .when(this.userProvider)
                .getUser();

        // Mock the same esClient call with different arguments & return values
        doReturn(conceptsResponse)
                .when(this.esClient)
                .search(argThat(i -> isConceptQuery(i)), any());
        doReturn(vocabulariesResponse)
                .when(this.esClient)
                .search(argThat(i -> isVocabularyQuery(i)), any());

        // Actual query
        var request = new TerminologySearchRequest();
        request.setQuery("test");
        request.setSearchConcepts(true); // true == additional concept search
        var response = service.searchTerminology(request);
        assertNotNull(response);

        // Make sure the mocks were called as expected

        verify(this.userProvider, times(2))
                .getUser();

        ArgumentCaptor<SearchRequest> srCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(this.esClient, times(2))
                .search(srCaptor.capture(), any(RequestOptions.class));

        // Check that esClient.search was called with expected kinds of arguments
        var args = srCaptor.getAllValues();
        assertEquals(2, args.size());

        var conceptsRequest = args.get(0);
        assertEquals("concepts",
                String.join(",", conceptsRequest.indices()));

        var vocabulariesRequest = args.get(1);
        assertEquals("vocabularies",
                String.join(",", vocabulariesRequest.indices()));

        verifyNoMoreInteractions(esClient);

        // parsed request should contain 1 terminology
        assertEquals(1, response.getTotalHitCount());

        assertNoLogErrors();
    }

    @Test
    public void testOrganizationQuery() throws Exception {
        var request = new TerminologySearchRequest();
        request.setQuery("foo");

        var vocabulariesEsResponse = EsUtils.getMockResponse(
                "/es/response/vocabulary_response.json");

        var orgId = UUID.randomUUID();
        Map<UUID, Set<Role>> roles = new HashMap<>();
        roles.put(orgId, Set.of(Role.ADMIN));

        doReturn(vocabulariesEsResponse)
                .when(this.esClient)
                .search(argThat(i -> isVocabularyQuery(i)), any());
        doReturn(this.createMockUser(false, roles))
            .when(userProvider)
            .getUser();

        TerminologySearchResponse response = service.searchTerminology(request);

        ArgumentCaptor<SearchRequest> srCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(this.esClient)
                .search(srCaptor.capture(), any(RequestOptions.class));

        var values = srCaptor.getAllValues();

        // query should include organization id
        assertTrue(values.get(0).source().toString().indexOf("references.contributor.id\":[\"" + orgId.toString()) > -1);
    }

}
