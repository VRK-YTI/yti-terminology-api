package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.mapper.MapperUtils;
import fi.vm.yti.common.properties.DCAP;
import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.dto.TerminologyInfoDTO;
import fi.vm.yti.terminology.api.v2.opensearch.IndexTerminology;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

public class TerminologyMapper {

    private TerminologyMapper() {
        // only static methods
    }

    public static Model dtoToModel(TerminologyDTO dto, String graph) {
        var model = ModelFactory.createDefaultModel();
        model.createResource(graph)
                .addProperty(DCAP.preferredXMLNamespacePrefix, dto.getPrefix())
                .addProperty(RDF.type, SKOS.ConceptScheme)
                .addProperty(RDFS.label, "Testisanasto");

        return model;
    }

    public static TerminologyInfoDTO modelToDTO(Model model) {
        var dto = new TerminologyInfoDTO();
        var terminologyURI = TerminologyURI.createTerminologyURI(model);
        dto.setPrefix(terminologyURI.getPrefix());
        dto.setUri(terminologyURI.getGraphURI());

        return dto;
    }

    public static IndexTerminology toIndexDocument(Model model) {
        var terminologyURI = TerminologyURI.createTerminologyURI(model);
        var terminologyResource = model.getResource(terminologyURI.getGraphURI());

        var index = new IndexTerminology();
        index.setUri(terminologyURI.getGraphURI());
        index.setId(terminologyURI.getGraphURI());
        index.setLabel(MapperUtils.localizedPropertyToMap(terminologyResource, RDFS.label));

        return index;
    }
}
