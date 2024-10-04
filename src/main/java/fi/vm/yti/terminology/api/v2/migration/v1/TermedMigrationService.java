package fi.vm.yti.terminology.api.v2.migration.v1;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.service.FrontendService;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.AuthorizationException;
import fi.vm.yti.terminology.api.v2.mapper.ConceptCollectionMapper;
import fi.vm.yti.terminology.api.v2.mapper.ConceptMapper;
import fi.vm.yti.terminology.api.v2.mapper.TerminologyMapper;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.service.TerminologyService;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
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

@Service
public class TermedMigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(TermedMigrationService.class);

    private final TerminologyService terminologyService;

    private final FrontendService frontendService;

    private final TerminologyRepository terminologyRepository;

    private final WebClient webClient;
    private final AuthenticatedUserProvider userProvider;

    public TermedMigrationService(TerminologyService terminologyService,
                                  TerminologyRepository terminologyRepository,
                                  AuthenticatedUserProvider userProvider,
                                  FrontendService frontendService,
                                  @Value("${api.url:http://localhost:9102/api}") String termedHost,
                                  @Value("${api.user:}") String termedUser,
                                  @Value("${api.pw:}") String termedPassword) {
        this.terminologyService = terminologyService;
        this.terminologyRepository = terminologyRepository;
        this.userProvider = userProvider;
        this.frontendService = frontendService;

        HttpHeaders defaultHttpHeaders = new HttpHeaders();
        defaultHttpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        defaultHttpHeaders.setBasicAuth(termedUser, termedPassword);

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(200 * 1024 * 1024))
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
    public void migrateAll() {
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
                LOG.info("Migrate graph {}, ({}/{})", id, count, graphs.size());
                migrate(id);
                count++;
            }
        }
    }

    @Async
    public void migrate(final String terminologyId) {
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

        var categories = frontendService.getServiceCategories("fi");
        var graphURI = TerminologyURI.createTerminologyURI(terminologyDTO.getPrefix()).getGraphURI();

        var model = TerminologyMapper.dtoToModel(terminologyDTO, graphURI, categories, userProvider.getUser());
        fixMetadata(terminologyNode, model.getModelResource());

        handleConceptTermedData(model, terminologyId, defaultLanguage);

        if (!model.listSubjectsWithProperty(RDF.type, SKOS.Concept).hasNext()) {
            LOG.info("Skip empty terminology {}", terminologyId);
            return;
        }
        handleCollectionTermedData(model, terminologyId);

        terminologyRepository.put(graphURI, model);

        LOG.info("Terminology {} migrated", terminologyId);
    }

    private void handleConceptTermedData(ModelWrapper model, String terminologyId, String defaultLanguage) {
        var result = getTermedDataByType(terminologyId, "Concept");
        var user = userProvider.getUser();
        if (result != null && !result.isEmpty()) {
            result.iterator().forEachRemaining(concept -> {
                try {
                    var dto = TermedDataMapper.mapConcept(concept, defaultLanguage);
                    ConceptMapper.dtoToModel(model, dto, user);

                    var resource = model.getResourceById(dto.getIdentifier());
                    fixMetadata(concept, resource);
                } catch (Exception e) {
                    LOG.error("MIGRATION ERROR concept " + concept.get("uri").asText(), e);
                }
            });
        }
    }

    private void handleCollectionTermedData(ModelWrapper model, String terminologyId) {
        var result = getTermedDataByType(terminologyId, "Collection");
        var user = userProvider.getUser();

        if (result != null && !result.isEmpty()) {
            result.iterator().forEachRemaining(collection -> {
                try {
                    var dto = TermedDataMapper.mapCollection(collection);
                    ConceptCollectionMapper.dtoToModel(model, dto, user);
                    fixMetadata(collection, model.getResourceById(dto.getIdentifier()));
                } catch (Exception e) {
                    LOG.error("MIGRATION ERROR collection " + collection.get("uri").asText(), e);
                }
            });
        }
    }

    private static void fixMetadata(JsonNode node, Resource resource) {
        var oldData = new TermedDataParser(node);
        var created = oldData.getString("createdDate");
        var creator = oldData.getString("createdBy");
        var modified = oldData.getString("lastModifiedDate");
        var modifier = oldData.getString("lastModifiedBy");

        resource.removeAll(DCTerms.creator)
                .removeAll(DCTerms.created)
                .removeAll(DCTerms.modified)
                .removeAll(SuomiMeta.modifier);

        resource.addProperty(DCTerms.created, ResourceFactory.createTypedLiteral(created));
        resource.addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(modified));
        resource.addProperty(DCTerms.creator, creator);
        resource.addProperty(SuomiMeta.modifier, modifier);
        resource.addProperty(Termed.id, node.get("id").asText());
    }

    private JsonNode getTermedDataByType(String terminologyId, String type) {
        return webClient.get().uri(builder -> builder
                .pathSegment(terminologyId, "node-trees")
                .queryParam("select", "*")
                .queryParam("where", "type.id:" + type)
                .queryParam("max", "-1")
                .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
    }
}
