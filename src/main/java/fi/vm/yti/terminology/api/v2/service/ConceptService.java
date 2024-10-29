package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.exception.ResourceExistsException;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.service.AuditService;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptInfoDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptReferenceInfoDTO;
import fi.vm.yti.terminology.api.v2.exception.ResourceInUseException;
import fi.vm.yti.terminology.api.v2.mapper.ConceptMapper;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.SKOSXL;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.function.Consumer;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class ConceptService {

    private final TerminologyRepository repository;
    private final TerminologyAuthorizationManager authorizationManager;
    private final IndexService indexService;
    private final AuditService auditService;
    private final GroupManagementService groupManagementService;

    public ConceptService(TerminologyRepository repository,
                          TerminologyAuthorizationManager authorizationManager,
                          IndexService indexService,
                          GroupManagementService groupManagementService) {
        this.repository = repository;
        this.authorizationManager = authorizationManager;
        this.indexService = indexService;
        this.groupManagementService = groupManagementService;

        this.auditService = new AuditService("CONCEPT");
    }

    public ConceptInfoDTO get(String prefix, String conceptIdentifier) {
        var model = repository.fetchByPrefix(prefix);

        var resourceURI = TerminologyURI.createConceptURI(prefix, conceptIdentifier).getResourceURI();
        if (!model.containsId(conceptIdentifier)) {
            throw new ResourceNotFoundException(resourceURI);
        }

        Consumer<ResourceCommonInfoDTO> mapUser = null;
        if (authorizationManager.hasRightsToTerminology(prefix, model)) {
            mapUser = groupManagementService.mapUser();
        }

        var dto = ConceptMapper.modelToDTO(model, conceptIdentifier, mapUser);
        mapExternalReferenceLabels(dto);
        mapCollections(dto, model);
        return dto;
    }

    private void mapCollections(ConceptInfoDTO dto, ModelWrapper model) {
        SelectBuilder select = new SelectBuilder();
        select.addGraph(NodeFactory.createURI(model.getGraphURI()), new WhereBuilder().addWhere(
                "?collection", SKOS.member, NodeFactory.createURI(dto.getUri())));

        var collections = new ArrayList<String>();
        repository.querySelect(select.build(), (var row) -> collections.add(row.get("collection").toString()));

        collections.forEach(collection -> {
            var resource = model.getResource(collection);
            var ref = new ConceptReferenceInfoDTO();
            ref.setLabel(MapperUtils.localizedPropertyToMap(resource, SKOS.prefLabel));
            ref.setReferenceURI(resource.getURI());
            ref.setIdentifier(resource.getLocalName());
            ref.setPrefix(model.getPrefix());
            dto.getMemberOf().add(ref);
        });
    }

    private void mapExternalReferenceLabels(ConceptInfoDTO dto) {
        var externalRefs = new ArrayList<ConceptReferenceInfoDTO>();
        externalRefs.addAll(dto.getBroadMatch());
        externalRefs.addAll(dto.getNarrowMatch());
        externalRefs.addAll(dto.getRelatedMatch());
        externalRefs.addAll(dto.getExactMatch());
        externalRefs.addAll(dto.getCloseMatch());

        if (externalRefs.isEmpty()) {
            return;
        }

        var conceptVar = "?concept";
        var schemeVar = "?scheme";
        var builder = new ConstructBuilder();
        builder
                .addConstruct(conceptVar, SKOSXL.literalForm, "?label")
                .addConstruct(schemeVar, SKOS.prefLabel, "?terminologyLabel");

        var where = new WhereBuilder()
                .addWhere(conceptVar, SKOS.prefLabel, "?prefLabel")
                .addWhere("?prefLabel", SKOSXL.literalForm, "?label")
                .addWhere(conceptVar, SKOS.inScheme, schemeVar)
                .addWhere(schemeVar, SKOS.prefLabel, "?terminologyLabel");

        externalRefs.forEach(r -> builder.addWhereValueVar(conceptVar,
                ResourceFactory.createResource(r.getReferenceURI())));
        builder.addGraph("?g", where);

        var result = repository.queryConstruct(builder.build());

        externalRefs.forEach(ref -> {
            var extResource = result.getResource(ref.getReferenceURI());
            var terminologyResource = result.getResource(extResource.getNameSpace());
            ref.setLabel(MapperUtils.localizedPropertyToMap(extResource, SKOSXL.literalForm));
            ref.setTerminologyLabel(MapperUtils.localizedPropertyToMap(terminologyResource, SKOS.prefLabel));
        });
    }

    public URI create(String prefix, ConceptDTO dto) throws URISyntaxException {
        var model = repository.fetchByPrefix(prefix);
        var user = authorizationManager.getUser();
        check(authorizationManager.hasRightsToTerminology(prefix, model));

        var resourceURI = TerminologyURI.createConceptURI(prefix, dto.getIdentifier()).getResourceURI();

        if (repository.resourceExistsInGraph(model.getGraphURI(), resourceURI)) {
            throw new ResourceExistsException(dto.getIdentifier(), model.getGraphURI());
        }

        ConceptMapper.dtoToModel(model, dto, user);

        repository.put(model.getGraphURI(), model);
        indexService.addConceptToIndex(ConceptMapper.toIndexDocument(model, dto.getIdentifier()));
        auditService.log(AuditService.ActionType.CREATE, resourceURI, user);

        return new URI(resourceURI);
    }

    public void update(String prefix, String conceptIdentifier, ConceptDTO dto) {
        var model = repository.fetchByPrefix(prefix);
        var user = authorizationManager.getUser();
        check(authorizationManager.hasRightsToTerminology(prefix, model));

        var resourceURI = TerminologyURI.createConceptURI(prefix, conceptIdentifier).getResourceURI();

        if (!repository.resourceExistsInGraph(model.getGraphURI(), resourceURI)) {
            throw new ResourceNotFoundException(resourceURI);
        }

        ConceptMapper.dtoToUpdateModel(model, conceptIdentifier, dto, user);

        repository.put(model.getGraphURI(), model);
        indexService.updateConceptToIndex(ConceptMapper.toIndexDocument(model, conceptIdentifier));
        auditService.log(AuditService.ActionType.UPDATE, resourceURI, user);
    }

    public void delete(String prefix, String conceptIdentifier) {
        var model = repository.fetchByPrefix(prefix);
        check(authorizationManager.hasRightsToTerminology(prefix, model));

        var resourceURI = TerminologyURI.createConceptURI(prefix, conceptIdentifier).getResourceURI();

        if (!repository.resourceExistsInGraph(model.getGraphURI(), resourceURI)) {
            throw new ResourceNotFoundException(resourceURI);
        }

        checkResourceInUse(model, resourceURI);

        ConceptMapper.mapDeleteConcept(model, conceptIdentifier);
        repository.put(model.getGraphURI(), model);

        indexService.deleteConceptFromIndex(resourceURI);
        auditService.log(AuditService.ActionType.DELETE, resourceURI, authorizationManager.getUser());
    }

    public boolean exists(String prefix, String conceptIdentifier) {
        var u = TerminologyURI.createConceptURI(prefix, conceptIdentifier);
        return repository.resourceExistsInGraph(u.getGraphURI(), u.getResourceURI());
    }

    /**
     * Checks if concept added as a reference to other resources, e.g. skos:broader, skos:member...
     * @param model terminology model
     * @param resourceURI concept URI
     */
    private void checkResourceInUse(ModelWrapper model, String resourceURI) {
        var properties = new ArrayList<>(ConceptMapper.internalRefProperties);
        properties.add(SKOS.member);

        var builder = new ConstructBuilder();

        var w = new WhereBuilder();
        for (Property ref : properties) {
            var varName = "?" + ref.getLocalName();
            builder.addConstruct(varName, ref, NodeFactory.createURI(resourceURI));
            w.addOptional(new WhereBuilder()
                    .addWhere(varName, ref, NodeFactory.createURI(resourceURI)));
        }

        builder.addGraph(NodeFactory.createURI(model.getGraphURI()), w);

        var result = repository.queryConstruct(builder.build());

        var refList = new ArrayList<ResourceInUseException.ReferenceDetail>();
        result.listSubjects().forEach(subj ->
                subj.listProperties().forEach(prop -> {
                    var detail = new ResourceInUseException.ReferenceDetail();
                    detail.setProperty(prop.getPredicate().getLocalName());
                    detail.setUri(subj.getURI());
                    refList.add(detail);
        }));

        if (!refList.isEmpty()) {
            throw new ResourceInUseException(refList);
        }
    }
}
