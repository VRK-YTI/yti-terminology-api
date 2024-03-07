package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDFS;

public class TerminologyMapper {
    public static Model dtoToModel(TerminologyDTO dto, String graph) {
        Model model = ModelFactory.createDefaultModel();
        model.createResource(graph)
                .addProperty(RDFS.label, "Testisanasto");

        return model;
    }
}
