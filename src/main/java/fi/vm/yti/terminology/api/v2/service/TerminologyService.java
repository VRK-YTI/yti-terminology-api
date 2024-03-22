package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.dto.TerminologyInfoDTO;
import fi.vm.yti.terminology.api.v2.mapper.TerminologyMapper;
import fi.vm.yti.terminology.api.v2.opensearch.OpenSearchIndexer;
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
    private final OpenSearchIndexer openSearchIndexer;

    public TerminologyService(TerminologyRepository terminologyRepository,
                              TerminologyAuthorizationManager authorizationManager,
                              OpenSearchIndexer openSearchIndexer) {
        this.terminologyRepository = terminologyRepository;
        this.authorizationManager = authorizationManager;
        this.openSearchIndexer = openSearchIndexer;
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

        openSearchIndexer.addTerminologyToIndex(TerminologyMapper.toIndexDocument(model));
        return new URI(graphURI);
    }

    public void deleteTerminology(String prefix) {
        check(authorizationManager.isSuperUser());
        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();
        terminologyRepository.delete(graphURI);
    }
}
