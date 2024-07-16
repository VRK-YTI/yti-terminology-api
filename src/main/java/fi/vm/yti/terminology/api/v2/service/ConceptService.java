package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.exception.ResourceExistsException;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.service.AuditService;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptInfoDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptReferenceInfoDTO;
import fi.vm.yti.terminology.api.v2.mapper.ConceptMapper;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
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
        return dto;
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
        var builder = new ConstructBuilder();
        builder.addConstruct(conceptVar, SKOSXL.literalForm, "?label");
        var where = new WhereBuilder()
                .addWhere(conceptVar, SKOS.prefLabel, "?prefLabel")
                .addWhere("?prefLabel", SKOSXL.literalForm, "?label");

        externalRefs.forEach(r -> where.addWhereValueVar(conceptVar,
                ResourceFactory.createResource(r.getConceptURI())));
        builder.addWhere(where);

        var result = repository.queryConstruct(builder.build());

        externalRefs.forEach(ref -> {
            var extResource = result.getResource(ref.getConceptURI());
            ref.setLabel(MapperUtils.localizedPropertyToMap(extResource, SKOSXL.literalForm));
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

        ConceptMapper.mapDeleteConcept(model, conceptIdentifier);
        repository.put(model.getGraphURI(), model);

        indexService.deleteConceptFromIndex(resourceURI);
        auditService.log(AuditService.ActionType.DELETE, resourceURI, authorizationManager.getUser());
    }

    public boolean exists(String prefix, String conceptIdentifier) {
        var u = TerminologyURI.createConceptURI(prefix, conceptIdentifier);
        return repository.resourceExistsInGraph(u.getGraphURI(), u.getResourceURI());
    }
}
