package fi.vm.yti.terminology.api.v2.property;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class Term {

    private static final String URI = "https://iri.suomi.fi/model/term/";

    private Term() {
        // property class
    }

    public static final Property terminologyType = ResourceFactory.createProperty(URI, "terminologyType");
}
