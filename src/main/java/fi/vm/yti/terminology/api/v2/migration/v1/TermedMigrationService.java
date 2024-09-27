package fi.vm.yti.terminology.api.v2.migration.v1;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.common.Constants;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.AuthorizationException;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.service.ConceptCollectionService;
import fi.vm.yti.terminology.api.v2.service.ConceptService;
import fi.vm.yti.terminology.api.v2.service.TerminologyService;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.net.URISyntaxException;

@Service
public class TermedMigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(TermedMigrationService.class);

    @Value("${terminology.host:https://sanastot.test.yti.cloud.dvv.fi}")
    String terminologyHost;

    private final TerminologyService terminologyService;

    private final ConceptService conceptService;

    private final ConceptCollectionService collectionService;

    private final TerminologyRepository terminologyRepository;

    private final WebClient webClient;
    private final AuthenticatedUserProvider userProvider;

    public TermedMigrationService(TerminologyService terminologyService,
                                  ConceptService conceptService,
                                  TerminologyRepository terminologyRepository,
                                  AuthenticatedUserProvider userProvider,
                                  ConceptCollectionService collectionService,
                                  @Value("${api.url:http://localhost:9102/api}") String termedHost,
                                  @Value("${api.user:}") String termedUser,
                                  @Value("${api.pw:}") String termedPassword) {
        this.terminologyService = terminologyService;
        this.conceptService = conceptService;
        this.terminologyRepository = terminologyRepository;
        this.userProvider = userProvider;
        this.collectionService = collectionService;

        HttpHeaders defaultHttpHeaders = new HttpHeaders();
        defaultHttpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        defaultHttpHeaders.setBasicAuth(termedUser, termedPassword);

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
        this.webClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .defaultHeaders(headers -> headers.addAll(defaultHttpHeaders))
                .baseUrl(termedHost + "/graphs")
                .clientConnector(new ReactorClientHttpConnector(
                                HttpClient.create(ConnectionProvider.builder("terminology").build())
                        )
                ).build();
    }

    @Async
    public void migrateAll() throws URISyntaxException {
        var graphs = webClient.get()
                .uri(UriBuilder::build)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (graphs != null && !graphs.isEmpty()) {
            int count = 1;
            for (var graph : graphs) {
                // skip graphs without code, e.g. organization and category graphs
                if (graph.get("code") == null) {
                    continue;
                }
                var id = graph.get("id").asText();
                LOG.info("Migrate graph {}/{}, {}", id, count, graphs.size());
                migrate(id);
                count++;
            }
        }
    }

    @Async
    public void migrate(final String terminologyId) throws URISyntaxException {
        if (!userProvider.getUser().isSuperuser()) {
            throw new AuthorizationException("Not allowed");
        }

        var terminologyData = getTermedDataByType(terminologyId, "TerminologicalVocabulary");
        if (terminologyData == null || terminologyData.isEmpty()) {
            LOG.warn("Invalid terminology {}, no metadata available", terminologyId);
            return;
        }

        var terminologyNode = terminologyData.iterator().next();
        var terminologyDTO = TermedDataMapper.mapTerminology(terminologyNode);
        var defaultLanguage = terminologyDTO.getLanguages().contains("fi")
                ? "fi"
                : terminologyDTO.getLanguages().iterator().next();
        try {
            terminologyService.delete(terminologyDTO.getPrefix());
        } catch (Exception e) {
            // not yet added
        }

        terminologyService.create(terminologyDTO);

        updateTimestamps(terminologyNode, terminologyDTO.getPrefix(), null);
        addTermedId(terminologyDTO.getPrefix(), null, terminologyId);

        handleConceptTermedData(terminologyId, terminologyDTO.getPrefix(), defaultLanguage);
        handleCollectionTermedData(terminologyId, terminologyDTO.getPrefix());
        LOG.info("Terminology {} migrated", terminologyId);
    }

    private void handleConceptTermedData(String terminologyId, String prefix, String defaultLanguage) {
        var result = getTermedDataByType(terminologyId, "Concept");

        if (result != null && !result.isEmpty()) {
            result.iterator().forEachRemaining(concept -> {
                try {
                    var dto = TermedDataMapper.mapConcept(concept, defaultLanguage);

                    conceptService.create(prefix, dto);
                    updateTimestamps(concept, prefix, dto.getIdentifier());
                    addTermedId(prefix, dto.getIdentifier(), concept.get("id").asText());
                } catch (Exception e) {
                    LOG.error("MIGRATION ERROR concept " + concept.get("uri").asText(), e);
                }
            });
        }
    }

    private void handleCollectionTermedData(String terminologyId, String prefix) {
        var result = getTermedDataByType(terminologyId, "Collection");

        if (result != null && !result.isEmpty()) {
            result.iterator().forEachRemaining(collection -> {
                try {
                    var dto = TermedDataMapper.mapCollection(collection);

                    collectionService.create(prefix, dto);
                    updateTimestamps(collection, prefix, dto.getIdentifier());
                    addTermedId(prefix, dto.getIdentifier(), collection.get("id").asText());
                } catch (Exception e) {
                    LOG.error("MIGRATION ERROR collection " + collection.get("uri").asText(), e);
                }
            });
        }
    }

    private void updateTimestamps(JsonNode node, String prefix, String identifier) {
        TerminologyURI terminologyURI;
        Resource subject;
        if (identifier != null) {
            terminologyURI = TerminologyURI.createConceptURI(prefix, identifier);
            subject = ResourceFactory.createResource(terminologyURI.getResourceURI());
        } else {
            terminologyURI = TerminologyURI.createTerminologyURI(prefix);
            subject = ResourceFactory.createResource(terminologyURI.getModelResourceURI());
        }
        var graph = NodeFactory.createURI(terminologyURI.getGraphURI());
        
        var builder = new UpdateBuilder();
        builder.addPrefixes(Constants.PREFIXES);

        var oldData = new TermedDataParser(node);

        var created = oldData.getString("createdDate");
        var creator = oldData.getString("createdBy");
        var modified = oldData.getString("lastModifiedDate");
        var modifier = oldData.getString("lastModifiedBy");

        builder.addDelete(graph, subject, SuomiMeta.creator, "?creator")
                .addDelete(graph, subject, SuomiMeta.modifier, "?modifier")
                .addDelete(graph, subject, DCTerms.created, "?created")
                .addDelete(graph, subject, DCTerms.modified, "?modified")
                .addInsert(graph, subject, DCTerms.created, created)
                .addInsert(graph, subject, DCTerms.modified, modified)
                .addInsert(graph, subject, SuomiMeta.creator, creator)
                .addInsert(graph, subject, SuomiMeta.modifier, modifier);

        var whereBuilder = new WhereBuilder()
                .addWhere(subject, SuomiMeta.creator, "?creator")
                .addWhere(subject, SuomiMeta.modifier, "?modifier")
                .addWhere(subject, DCTerms.created, "?created")
                .addWhere(subject, DCTerms.modified, "?modified");

        builder.addGraph(graph, whereBuilder);
        
        terminologyRepository.queryUpdate(builder.buildRequest());
    }

    private void addTermedId(String prefix, String identifier, String termedId) {
        TerminologyURI terminologyURI;
        Resource subject;
        if (identifier != null) {
            terminologyURI = TerminologyURI.createConceptURI(prefix, identifier);
            subject = ResourceFactory.createResource(terminologyURI.getResourceURI());
        } else {
            terminologyURI = TerminologyURI.createTerminologyURI(prefix);
            subject = ResourceFactory.createResource(terminologyURI.getModelResourceURI());
        }

        var builder = new UpdateBuilder();
        builder.addPrefixes(Constants.PREFIXES);
        var graph = NodeFactory.createURI(terminologyURI.getGraphURI());

        builder.addInsert(graph, subject, Termed.id, termedId);

        terminologyRepository.queryUpdate(builder.buildRequest());
    }

    private JsonNode getTermedDataByType(String terminologyId, String type) {
        return webClient.get().uri(builder -> builder
                .pathSegment(terminologyId, "node-trees")
                .queryParam("select", "*")
                .queryParam("where", "type.id:" + type)
                .queryParam("max", 10000)
                .build()).retrieve().bodyToMono(JsonNode.class).block();
    }
}
