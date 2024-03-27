package fi.vm.yti.terminology.api.v2.util;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.exception.MappingError;
import fi.vm.yti.common.properties.DCAP;
import fi.vm.yti.common.util.GraphURI;
import fi.vm.yti.common.util.MapperUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

public class TerminologyURI extends GraphURI {

    public static TerminologyURI createTerminologyURI(String prefix) {
        return new TerminologyURI(prefix);
    }

    public static TerminologyURI createTerminologyURI(Model model) {
        var subj = model.listSubjectsWithProperty(RDF.type, SKOS.ConceptScheme);
        if (!subj.hasNext()) {
            throw new MappingError("Not a valid model");
        }
        return new TerminologyURI(MapperUtils.propertyToString(subj.next(), DCAP.preferredXMLNamespacePrefix));
    }

    public static TerminologyURI createConceptURI(String prefix, String conceptId) {
        return new TerminologyURI(prefix, conceptId);
    }

    private TerminologyURI(String prefix) {
        createModelURI(prefix, null);
    }

    private TerminologyURI(String prefix, String conceptId) {
        createResourceURI(prefix, conceptId, null);
    }

    @Override
    public String getGraphURI() {
        return getModelResourceURI();
    }

    @Override
    public String getModelResourceURI() {
        return Constants.TERMINOLOGY_NAMESPACE + getPrefix() + Constants.RESOURCE_SEPARATOR;
    }
}
