package fi.vm.yti.terminology.api.v2.migration.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.common.Constants;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.service.FrontendService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.AuthorizationException;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.LocalizedValueDTO;
import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.mapper.ConceptMapper;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.service.ConceptCollectionService;
import fi.vm.yti.terminology.api.v2.service.ConceptService;
import fi.vm.yti.terminology.api.v2.service.TerminologyService;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TermedMigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(TermedMigrationService.class);

    @Value("${terminology.host:https://sanastot.test.yti.cloud.dvv.fi}")
    String terminologyHost;

    private final TerminologyService terminologyService;

    private final ConceptService conceptService;

    private final FrontendService frontendService;

    private final ConceptCollectionService collectionService;

    private final TerminologyRepository terminologyRepository;

    private final WebClient webClient;
    private final AuthenticatedUserProvider userProvider;

    private static final ObjectMapper mapper = new ObjectMapper();

    public TermedMigrationService(TerminologyService terminologyService,
                                  ConceptService conceptService,
                                  FrontendService frontendService,
                                  TerminologyRepository terminologyRepository,
                                  AuthenticatedUserProvider userProvider,
                                  ConceptCollectionService collectionService,
                                  @Value("${api.url:http://localhost:9102/api}") String termedHost,
                                  @Value("${api.user:}") String termedUser,
                                  @Value("${api.pw:}") String termedPassword) {
        this.terminologyService = terminologyService;
        this.conceptService = conceptService;
        this.frontendService = frontendService;
        this.terminologyRepository = terminologyRepository;
        this.userProvider = userProvider;
        this.collectionService = collectionService;

        HttpHeaders defaultHttpHeaders = new HttpHeaders();
        defaultHttpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        defaultHttpHeaders.setBasicAuth(termedUser, termedPassword);

        this.webClient = WebClient.builder()
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
            for (var graph : graphs) {
                // skip graphs without code, e.g. organization and category graphs
                if (graph.get("code") == null) {
                    continue;
                }
                var id = graph.get("id").asText();
                LOG.info("Migrate graph {}", id);
                migrate(id);
            }
        }
    }

    public void migrate(final String terminologyId) throws URISyntaxException {
        if (!userProvider.getUser().isSuperuser()) {
            throw new AuthorizationException("Not allowed");
        }

        var oldData = ModelFactory.createDefaultModel();

        RDFParser.create()
                .source(terminologyHost + "/terminology-api/api/v1/export/" + terminologyId + "?format=rdf")
                .lang(Lang.RDFXML)
                .parse(oldData);

        var terminologyMeta = oldData.listSubjectsWithProperty(RDF.type, SKOS.ConceptScheme);

        if (!terminologyMeta.hasNext()) {
            LOG.warn("Invalid terminology {}, no metadata available", terminologyId);
            return;
        }

        var metaResource = terminologyMeta.next();
        var allCategories = frontendService.getServiceCategories("fi");

        var terminologyDTO = TermedDataMapper.mapTerminology(metaResource, allCategories);

        var defaultLanguage = terminologyDTO.getLanguages().contains("fi")
                ? "fi"
                : terminologyDTO.getLanguages().iterator().next();

        try {
            terminologyService.delete(terminologyDTO.getPrefix());
        } catch (Exception e) {
            // not yet added
        }

        terminologyService.create(terminologyDTO);

        updateTimestamps(metaResource,terminologyDTO.getPrefix(), null);
        addTermedId(terminologyDTO.getPrefix(), null, terminologyId);

        handleConcepts(terminologyId, oldData, terminologyDTO, defaultLanguage);
        handleCollections(terminologyDTO.getPrefix(), oldData);
    }

    private void handleConcepts(String terminologyId, Model oldData, TerminologyDTO terminologyDTO, String defaultLanguage) {
        oldData.listSubjectsWithProperty(RDF.type, SKOS.Concept)
                .filterKeep(c -> c.getURI().startsWith(TermedDataMapper.URI_SUOMI_FI))
                .forEach(resource -> {
                    var concept = TermedDataMapper.mapConcept(oldData, resource, mapper, defaultLanguage);

                    // fetch data from termed because export doesn't maintain the order
                    if (!concept.getNotes().isEmpty() || !concept.getExamples().isEmpty()) {
                        var conceptTermedId = MapperUtils.propertyToString(resource, Termed.id);
                        var conceptResult = webClient.get().uri(builder -> builder
                                .pathSegment(terminologyId, "node-trees")
                                .queryParam("select", "id,properties.*")
                                .queryParam("where", "id:" + conceptTermedId)
                                .build()).retrieve().bodyToMono(JsonNode.class).block();

                        if (conceptResult != null && !conceptResult.isEmpty()) {
                            JsonNode properties = conceptResult.get(0).get("properties");
                            concept.setNotes(handleOrder(properties, "note"));
                            concept.setExamples(handleOrder(properties, "example"));
                        }
                    }

                    ConceptMapper.externalRefProperties.forEach(prop ->
                            MapperUtils.arrayPropertyToList(resource, prop)
                                    .forEach(r -> {
                                        var conceptLink = oldData.getResource(r);
                                        var linkedTerminology = MapperUtils.propertyToString(conceptLink, Termed.targetUri);
                                        var linkedConcept = MapperUtils.propertyToString(conceptLink, Termed.targetId);

                                        LOG.info("Fetch from termed: /api/graphs/{}/node-trees?select=*&where=id:{}", linkedTerminology, linkedConcept);

                                        var result = webClient.get().uri(builder -> builder
                                                .pathSegment(linkedTerminology, "node-trees")
                                                .queryParam("select", "*")
                                                .queryParam("where", "id:" + linkedConcept)
                                                .build()).retrieve().bodyToMono(JsonNode.class).block();

                                        if (result != null && !result.isEmpty()) {
                                            var refURI = TermedDataMapper.fixURI(result.get(0).get("uri").asText());
                                            if (prop.equals(SKOS.broadMatch)) {
                                                concept.getBroadMatch().add(refURI);
                                            } else if (prop.equals(SKOS.narrowMatch)) {
                                                concept.getNarrowMatch().add(refURI);
                                            } else if (prop.equals(SKOS.relatedMatch)) {
                                                concept.getRelatedMatch().add(refURI);
                                            } else if (prop.equals(SKOS.exactMatch)) {
                                                concept.getExactMatch().add(refURI);
                                            } else if (prop.equals(SKOS.closeMatch)) {
                                                concept.getCloseMatch().add(refURI);
                                            }
                                        }
                                    }));
                    try {
                        conceptService.create(terminologyDTO.getPrefix(), concept);
                        updateTimestamps(resource, terminologyDTO.getPrefix(), concept.getIdentifier());
                        addTermedId(terminologyDTO.getPrefix(), concept.getIdentifier(), MapperUtils.propertyToString(resource, Termed.id));
                    } catch (URISyntaxException e) {
                        LOG.error(e.getMessage(), e);
                    }
                });
    }

    private void handleCollections(String prefix, Model oldData) {
        oldData.listSubjectsWithProperty(RDF.type, SKOS.Collection).forEach(c -> {
            var label = MapperUtils.localizedPropertyToMap(c, SKOS.prefLabel);
            var description = MapperUtils.localizedPropertyToMap(c, SKOS.definition);
            var identifier = NodeFactory.createURI(MapperUtils.propertyToString(c, Termed.uri)).getLocalName();
            var members = MapperUtils.arrayPropertyToList(c, SKOS.member).stream()
                    .map(TermedDataMapper::fixURI)
                    .collect(Collectors.toSet());

            var dto = new ConceptCollectionDTO();
            dto.setIdentifier(identifier);
            dto.setLabel(label);
            dto.setDescription(description);
            dto.setMembers(members);
            try {
                collectionService.create(prefix, dto);
                updateTimestamps(c, prefix, identifier);
                addTermedId(prefix, identifier, MapperUtils.propertyToString(c, Termed.id));
            } catch (Exception e) {
                LOG.error("Error creating collection", e);
            }
        });
    }

    private static List<LocalizedValueDTO> handleOrder(JsonNode properties, String property) {
        var result = new ArrayList<LocalizedValueDTO>();
        Optional.ofNullable(
                properties.get(property))
                .ifPresent(prop -> prop.iterator().forEachRemaining(p -> result.add(
                        new LocalizedValueDTO(p.get("lang").asText(), p.get("value").asText()))));
        return result;
    }

    private void updateTimestamps(Resource oldResource, String prefix, String identifier) {
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

        var created = MapperUtils.getLiteral(oldResource, Termed.createdDate, String.class);
        var creator = MapperUtils.getLiteral(oldResource, Termed.createdBy, String.class);
        var modified = MapperUtils.getLiteral(oldResource, Termed.lastModifiedDate, String.class);
        var modifier = MapperUtils.getLiteral(oldResource, Termed.lastModifiedBy, String.class);

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
}
