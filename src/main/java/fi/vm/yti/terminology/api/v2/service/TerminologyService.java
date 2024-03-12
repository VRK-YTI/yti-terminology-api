package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.dto.TerminologyInfoDTO;
import fi.vm.yti.terminology.api.v2.mapper.TerminologyMapper;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class TerminologyService {

    private final TerminologyRepository terminologyRepository;
    private final TerminologyAuthorizationManager authorizationManager;

    public TerminologyService(TerminologyRepository terminologyRepository,
                              TerminologyAuthorizationManager authorizationManager) {
        this.terminologyRepository = terminologyRepository;
        this.authorizationManager = authorizationManager;
    }

    public TerminologyInfoDTO getTerminology(String prefix) {
        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();
        Model model = terminologyRepository.fetch(graphURI);
        return TerminologyMapper.modelToDTO(model);
    }

    public URI creteTerminology(TerminologyDTO dto) throws URISyntaxException {
        check(authorizationManager.hasRightToAnyOrganization(dto.getOrganizations()));
        var graphURI = TerminologyURI.createTerminologyURI(dto.getPrefix()).getGraphURI();
        var model = TerminologyMapper.dtoToModel(dto, graphURI);
        terminologyRepository.put(graphURI, model);

        return new URI(graphURI);
    }

    public void deleteTerminology(String prefix) {
        check(authorizationManager.isSuperUser());
        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();
        terminologyRepository.delete(graphURI);
    }
}
