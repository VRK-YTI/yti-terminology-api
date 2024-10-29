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

    public static final Property homographNumber = ResourceFactory.createProperty(URI, "homographNumber");
    public static final Property termInfo = ResourceFactory.createProperty(URI, "termInfo");
    public static final Property scope = ResourceFactory.createProperty(URI, "scope");
    public static final Property termStyle = ResourceFactory.createProperty(URI, "termStyle");
    public static final Property termFamily = ResourceFactory.createProperty(URI, "termFamily");
    public static final Property termConjugation = ResourceFactory.createProperty(URI, "termConjugation");
    public static final Property termEquivalency = ResourceFactory.createProperty(URI, "termEquivalency");
    public static final Property wordClass = ResourceFactory.createProperty(URI, "wordClass");
    public static final Property notRecommendedSynonym = ResourceFactory.createProperty(URI, "notRecommendedSynonym");

    public static final Property orderedSynonym = ResourceFactory.createProperty(URI, "orderedSynonym");
    public static final Property orderedNotRecommendedSynonym = ResourceFactory.createProperty(URI, "orderedNotRecommendedSynonym");
    public static final Property orderedMember = ResourceFactory.createProperty(URI, "orderedMember");
    public static final Property orderedBroader = ResourceFactory.createProperty(URI, "orderedBroader");
    public static final Property orderedNarrower = ResourceFactory.createProperty(URI, "orderedNarrower");
    public static final Property orderedRelated = ResourceFactory.createProperty(URI, "orderedRelated");
    public static final Property orderedIsPartOf = ResourceFactory.createProperty(URI, "orderedIsPartOf");
    public static final Property orderedHasPart = ResourceFactory.createProperty(URI, "orderedHasPart");
    public static final Property orderedRelatedMatch = ResourceFactory.createProperty(URI, "orderedRelatedMatch");
    public static final Property orderedBroadMatch = ResourceFactory.createProperty(URI, "orderedBroadMatch");
    public static final Property orderedNarrowMatch = ResourceFactory.createProperty(URI, "orderedNarrowMatch");
    public static final Property orderedExactMatch = ResourceFactory.createProperty(URI, "orderedExactMatch");
    public static final Property orderedCloseMatch = ResourceFactory.createProperty(URI, "orderedCloseMatch");
    public static final Property orderedNote =  ResourceFactory.createProperty(URI, "orderedNote");
    public static final Property orderedExample =  ResourceFactory.createProperty(URI, "orderedExample");
    public static final Property orderedEditorialNote =  ResourceFactory.createProperty(URI, "orderedEditorialNote");
    public static final Property orderedSource =  ResourceFactory.createProperty(URI, "orderedSource");

    public static String getNamespace() {
        return URI;
    }
}
