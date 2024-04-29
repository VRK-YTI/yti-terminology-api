package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.dto.MetaDataDTO;
import fi.vm.yti.common.properties.DCAP;
import fi.vm.yti.common.util.MapperUtils;
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

    public static Model dtoToModel(MetaDataDTO dto, String graph) {
        var model = ModelFactory.createDefaultModel();
        var resource = model.createResource(graph)
                .addProperty(DCAP.preferredXMLNamespacePrefix, dto.getPrefix())
                .addProperty(RDF.type, SKOS.ConceptScheme);

        MapperUtils.addLocalizedProperty(dto.getLanguages(), dto.getLabel(), resource, SKOS.prefLabel);

        return model;
    }

    public static TerminologyInfoDTO modelToDTO(Model model) {
        var dto = new TerminologyInfoDTO();
        var terminologyURI = TerminologyURI.createTerminologyURI(model);
        var resource = model.getResource(terminologyURI.getModelResourceURI());
        dto.setPrefix(terminologyURI.getPrefix());
        dto.setUri(terminologyURI.getGraphURI());
        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, SKOS.prefLabel));
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
