package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.exception.ResourceExistsException;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.service.AuditService;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionInfoDTO;
import fi.vm.yti.terminology.api.v2.mapper.ConceptCollectionMapper;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class ConceptCollectionService {
    private final TerminologyRepository repository;
    private final TerminologyAuthorizationManager authorizationManager;
    private final AuditService auditService;
    private final GroupManagementService groupManagementService;

    public ConceptCollectionService(TerminologyRepository repository,
                                    TerminologyAuthorizationManager authorizationManager,
                                    GroupManagementService groupManagementService) {
        this.repository = repository;
        this.authorizationManager = authorizationManager;
        this.groupManagementService = groupManagementService;
        this.auditService = new AuditService("CONCEPTCOLLECTION");
    }

    public List<ConceptCollectionInfoDTO> list(String prefix) {
        var model = repository.fetchByPrefix(prefix);

        return model.listSubjectsWithProperty(RDF.type, SKOS.Collection)
            .mapWith(s -> ConceptCollectionMapper.modelToDTO(model, s.getLocalName(), null))
            .toList();
    }

    public ConceptCollectionInfoDTO get(String prefix, String conceptCollectionIdentifier) {
        var model = repository.fetchByPrefix(prefix);

        var resourceURI = TerminologyURI.Factory.createConceptCollectionURI(prefix, conceptCollectionIdentifier).getResourceURI();
        if (!model.containsId(conceptCollectionIdentifier)) {
            throw new ResourceNotFoundException(resourceURI);
        }

        Consumer<ResourceCommonInfoDTO> mapUser = null;
        if (authorizationManager.hasRightsToTerminology(prefix, model)) {
            mapUser = groupManagementService.mapUser();
        }

        return ConceptCollectionMapper.modelToDTO(
                model,
                conceptCollectionIdentifier,
                mapUser);
    }

    public URI create(String prefix, ConceptCollectionDTO dto) throws URISyntaxException {
        var model = repository.fetchByPrefix(prefix);
        var user = authorizationManager.getUser();
        check(authorizationManager.hasRightsToTerminology(prefix, model));

        var resourceURI = TerminologyURI.Factory.createConceptCollectionURI(
                prefix,
                dto.getIdentifier()).getResourceURI();

        if (repository.resourceExistsInGraph(model.getGraphURI(), resourceURI)) {
            throw new ResourceExistsException(dto.getIdentifier(), model.getGraphURI());
        }

        ConceptCollectionMapper.dtoToModel(model, dto, user);

        repository.put(model.getGraphURI(), model);

        auditService.log(AuditService.ActionType.CREATE, resourceURI, user);

        return new URI(resourceURI);
    }

    public void update(
            String prefix,
            String conceptCollectionIdentifier,
            ConceptCollectionDTO dto) {
        var model = repository.fetchByPrefix(prefix);
        var user = authorizationManager.getUser();
        check(authorizationManager.hasRightsToTerminology(prefix, model));

        var resourceURI = TerminologyURI.Factory.createConceptCollectionURI(
                        prefix,
                        conceptCollectionIdentifier)
                .getResourceURI();
        if (!repository.resourceExistsInGraph(model.getGraphURI(), resourceURI)) {
            throw new ResourceNotFoundException(resourceURI);
        }

        ConceptCollectionMapper.dtoToUpdateModel(
                model,
                conceptCollectionIdentifier,
                dto,
                user);

        repository.put(model.getGraphURI(), model);

        auditService.log(AuditService.ActionType.UPDATE, resourceURI, user);
    }

    public void delete(String prefix, String conceptCollectionIdentifier) {
        var model = repository.fetchByPrefix(prefix);
        check(authorizationManager.hasRightsToTerminology(prefix, model));

        var resourceURI = TerminologyURI.Factory.createConceptCollectionURI(
                        prefix,
                        conceptCollectionIdentifier)
                .getResourceURI();

        if (!repository.resourceExistsInGraph(model.getGraphURI(), resourceURI)) {
            throw new ResourceNotFoundException(resourceURI);
        }

        ConceptCollectionMapper.mapDeleteConceptCollection(
                model,
                conceptCollectionIdentifier);
        repository.put(model.getGraphURI(), model);

        auditService.log(
                AuditService.ActionType.DELETE,
                resourceURI, authorizationManager.getUser());
    }

    public boolean exists(String prefix, String conceptCollectionIdentifier) {
        var u = TerminologyURI.Factory.createConceptCollectionURI(
                prefix,
                conceptCollectionIdentifier);
        return repository.resourceExistsInGraph(
                u.getGraphURI(),
                u.getResourceURI());
    }
}
