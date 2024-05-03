package fi.vm.yti.terminology.api.v2.property;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class Term {

    private static final String URI = "https://iri.suomi.fi/model/term/";

    private Term() {
        // property class
    }

    public static final Property terminologyType = ResourceFactory.createProperty(URI, "terminologyType");
    public static final Property conceptClass = ResourceFactory.createProperty(URI, "conceptClass");
    public static final Property subjectArea = ResourceFactory.createProperty(URI, "subjectArea");

    public static final Property source = ResourceFactory.createProperty(URI, "source");
    public static final Property homographNumber = ResourceFactory.createProperty(URI, "homographNumber");
    public static final Property termInfo = ResourceFactory.createProperty(URI, "termInfo");
    public static final Property scope = ResourceFactory.createProperty(URI, "scope");
    public static final Property termStyle = ResourceFactory.createProperty(URI, "termStyle");
    public static final Property termFamily = ResourceFactory.createProperty(URI, "termFamily");
    public static final Property termConjugation = ResourceFactory.createProperty(URI, "termConjugation");
    public static final Property termEquivalency = ResourceFactory.createProperty(URI, "termEquivalency");
    public static final Property wordClass = ResourceFactory.createProperty(URI, "wordClass");
    public static final Property notRecommendedSynonym = ResourceFactory.createProperty(URI, "notRecommendedSynonym");
}
