package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.mapper.TerminologyMapper;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import org.springframework.stereotype.Service;

@Service
public class TerminologyService {

    private final TerminologyRepository terminologyRepository;

    public TerminologyService(TerminologyRepository terminologyRepository) {
        this.terminologyRepository = terminologyRepository;
    }

    public void creteTerminology(TerminologyDTO dto) {
        String graph = "https://iri.suomi.fi/terminology/" + dto.getPrefix();
        var model = TerminologyMapper.dtoToModel(dto, graph);
        terminologyRepository.put(graph, model);
    }
}
