package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.service.AuditService;
import fi.vm.yti.common.service.FrontendService;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.terminology.api.v2.dto.StatusCountResponse;
import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.dto.TerminologyInfoDTO;
import fi.vm.yti.terminology.api.v2.mapper.TerminologyMapper;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.SKOSXL;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class TerminologyService {

    private final TerminologyRepository terminologyRepository;
    private final TerminologyAuthorizationManager authorizationManager;
    private final IndexService indexService;
    private final AuditService auditService;
    private final FrontendService frontendService;
    private final GroupManagementService groupManagementService;

    public TerminologyService(TerminologyRepository terminologyRepository,
                              TerminologyAuthorizationManager authorizationManager,
                              IndexService indexService,
                              FrontendService frontendService,
                              GroupManagementService groupManagementService) {
        this.terminologyRepository = terminologyRepository;
        this.authorizationManager = authorizationManager;
        this.indexService = indexService;
        this.frontendService = frontendService;
        this.groupManagementService = groupManagementService;

        this.auditService = new AuditService("TERMINOLOGY");
    }

    public TerminologyInfoDTO get(String prefix) {
        var uri = TerminologyURI.createTerminologyURI(prefix);
        var query = new ConstructBuilder()
                .addConstruct(NodeFactory.createURI(uri.getModelResourceURI()), "?p", "?o")
                .addGraph(NodeFactory.createURI(uri.getGraphURI()),
                        new WhereBuilder()
                                .addWhere(NodeFactory.createURI(uri.getModelResourceURI()), "?p", "?o"));

        var result = terminologyRepository.queryConstruct(query.build());
        var model = new ModelWrapper(result, uri.getGraphURI());

        Consumer<ResourceCommonInfoDTO> mapUser = null;
        if (authorizationManager.hasRightsToTerminology(prefix, model)) {
            mapUser = groupManagementService.mapUser();
        }
        return TerminologyMapper.modelToDTO(model,
                frontendService.getServiceCategories(),
                frontendService.getOrganizations("en", true),
                mapUser);
    }

    public URI create(TerminologyDTO dto) throws URISyntaxException {
        check(authorizationManager.hasRightToAnyOrganization(dto.getOrganizations()));
        var user = authorizationManager.getUser();

        var categories = frontendService.getServiceCategories();
        var graphURI = TerminologyURI.createTerminologyURI(dto.getPrefix()).getGraphURI();
        var model = TerminologyMapper.dtoToModel(dto, graphURI, categories, user);
        terminologyRepository.put(graphURI, model);

        indexService.addTerminologyToIndex(TerminologyMapper.toIndexDocument(model, categories));
        auditService.log(AuditService.ActionType.CREATE, graphURI, user);
        return new URI(graphURI);
    }

    public void update(String prefix, TerminologyDTO dto) {
        var model = terminologyRepository.fetchByPrefix(prefix);

        check(authorizationManager.hasRightsToTerminology(prefix, model));

        var user = authorizationManager.getUser();
        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();
        var categories = frontendService.getServiceCategories();

        TerminologyMapper.toUpdateModel(model, dto, categories, user);

        terminologyRepository.put(graphURI, model);
        indexService.updateTerminologyToIndex(TerminologyMapper.toIndexDocument(model, categories));
        auditService.log(AuditService.ActionType.UPDATE, graphURI, user);
    }

    public void delete(String prefix) {
        var model = terminologyRepository.fetchByPrefix(prefix);
        check(authorizationManager.hasRightsToTerminology(prefix, model));

        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();

        terminologyRepository.delete(graphURI);

        indexService.deleteTerminologyFromIndex(graphURI);
        auditService.log(AuditService.ActionType.DELETE, graphURI, authorizationManager.getUser());
    }

    public boolean exists(String prefix) {
        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();
        return terminologyRepository.graphExists(graphURI);
    }

    public ResponseEntity<String> export(String prefix, String accept) {

        ModelWrapper model;

        try {
            model = terminologyRepository.fetchByPrefix(prefix);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

        var hasRights = authorizationManager.hasRightsToTerminology(prefix, model);

        if (!hasRights) {
            if (Status.INCOMPLETE.equals(MapperUtils.getStatus(model.getModelResource()))) {
                return ResponseEntity.status(403).build();
            }

            var hiddenValues = model.listStatements(null, SKOS.editorialNote, (String) null);
            model.remove(hiddenValues);
        }

        var stringWriter = new StringWriter();

        switch (accept) {
            case "text/turtle":
                RDFDataMgr.write(stringWriter, model, Lang.TURTLE);
                break;
            case "application/rdf+xml":
                RDFDataMgr.write(stringWriter, model, Lang.RDFXML);
                break;
            case "application/ld+json":
            default:
                RDFDataMgr.write(stringWriter, model, Lang.JSONLD);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(stringWriter.toString());
    }

    public Long getConceptCollectionCount(String terminologyURI) {
        var query = new SelectBuilder().addVar("count(?s)", "count")
                .addGraph(NodeFactory.createURI(terminologyURI),
                        new WhereBuilder().addWhere("?s", RDF.type, SKOS.Collection))
                .build();

        var result = new ArrayList<Long>();
        terminologyRepository.querySelect(query, row -> result.add(row.get("count").asLiteral().getLong()));

        return result.isEmpty() ? 0L : result.get(0);
    }

    /**
     * Finds concepts' and terms' counts by type
     *
     * @param terminologyURI Terminology URI
     * @return counts
     */
    public StatusCountResponse getCountsByStatus(String terminologyURI) {
        var selectBuilder = new SelectBuilder();

        selectBuilder
                .addVar("?type")
                .addVar("?status")
                .addVar("count(?status)", "?count")
                .addGraph(NodeFactory.createURI(terminologyURI),
                        new WhereBuilder()
                                .addWhere("?s", RDF.type, "?type")
                                .addWhere("?s", SuomiMeta.publicationStatus, "?status"))
                .addValueVar("?type", SKOS.Concept, SKOSXL.Label)
                .addGroupBy("?type")
                .addGroupBy("?status");

        var conceptResult = new HashMap<String, Long>();
        var termResult = new HashMap<String, Long>();
        Arrays.stream(Status.values()).forEach(s -> {
            conceptResult.put(s.name(), 0L);
            termResult.put(s.name(), 0L);
        });
        terminologyRepository.querySelect(selectBuilder.build(), row -> {
            var type = row.get("type").asResource().getURI();
            var status = MapperUtils.getStatusFromUri(row.get("status").toString());
            var count = row.get("count").asLiteral().getLong();
            if (SKOS.Concept.getURI().equals(type)) {
                conceptResult.put(status.name(), count);
            } else if (SKOSXL.Label.getURI().equals(type)) {
                termResult.put(status.name(), count);
            }
        });

        var countResponse = new StatusCountResponse();
        countResponse.setConcepts(conceptResult);
        countResponse.setTerms(termResult);
        return countResponse;
    }
}
