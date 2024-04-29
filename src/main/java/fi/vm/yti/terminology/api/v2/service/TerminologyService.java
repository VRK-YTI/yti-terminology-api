package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.service.AuditService;
import fi.vm.yti.common.service.FrontendService;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.dto.TerminologyInfoDTO;
import fi.vm.yti.terminology.api.v2.mapper.TerminologyMapper;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
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
        var model = terminologyRepository.fetchByPrefix(prefix);
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
        var model = terminologyRepository.fetchByPrefix(dto.getPrefix());
        check(authorizationManager.hasRightsToTerminology(prefix, model));

        var user = authorizationManager.getUser();
        var graphURI = TerminologyURI.createTerminologyURI(dto.getPrefix()).getGraphURI();
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
}
