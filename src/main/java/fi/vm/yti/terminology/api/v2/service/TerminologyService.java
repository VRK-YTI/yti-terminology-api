package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.dto.*;
import fi.vm.yti.common.service.AbstractGraphService;
import fi.vm.yti.common.service.AuditService;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.v2.mapper.TerminologyMapper;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.atlas.lib.NotImplemented;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class TerminologyService extends AbstractGraphService<TerminologyRepository> {

    private final TerminologyRepository terminologyRepository;
    private final TerminologyAuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final IndexService indexService;
    private final AuditService auditService;

    public TerminologyService(TerminologyRepository terminologyRepository,
                              TerminologyAuthorizationManager authorizationManager,
                              AuthenticatedUserProvider userProvider,
                              IndexService indexService) {
        super(terminologyRepository);

        this.terminologyRepository = terminologyRepository;
        this.authorizationManager = authorizationManager;
        this.indexService = indexService;
        this.userProvider = userProvider;

        this.auditService = new AuditService("TERMINOLOGY");
    }

    @Override
    public MetaDataInfoDTO get(String prefix) {
        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();
        var model = terminologyRepository.fetch(graphURI);
        return TerminologyMapper.modelToDTO(model);
    }

    @Override
    public MetaDataInfoDTO get(String identifier, String version) {
        throw new NotImplemented("Version not supported");
    }

    @Override
    public URI create(MetaDataDTO dto) throws URISyntaxException {
        check(authorizationManager.hasRightToAnyOrganization(dto.getOrganizations()));
        var graphURI = TerminologyURI.createTerminologyURI(dto.getPrefix()).getGraphURI();
        var model = TerminologyMapper.dtoToModel(dto, graphURI);
        terminologyRepository.put(graphURI, model);

        indexService.addTerminologyToIndex(TerminologyMapper.toIndexDocument(model));
        auditService.log(AuditService.ActionType.CREATE, graphURI, userProvider.getUser());
        return new URI(graphURI);
    }

    @Override
    public void update(String prefix, MetaDataDTO dto) {
        throw new NotImplemented();
    }

    @Override
    public void delete(String identifier) {
        check(authorizationManager.isSuperUser());
        var graphURI = TerminologyURI.createTerminologyURI(identifier).getGraphURI();
        terminologyRepository.delete(graphURI);
    }

    @Override
    public void delete(String identifier, String version) {
        throw new NotImplemented("Version not supported");
    }
}
