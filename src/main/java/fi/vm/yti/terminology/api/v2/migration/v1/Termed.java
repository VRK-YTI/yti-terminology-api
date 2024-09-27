package fi.vm.yti.terminology.api.v2.migration.v1;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class Termed {

    private Termed () {}

    public static final Property status = ResourceFactory.createProperty("http://www.w3.org/2003/06/sw-vocab-status/ns#term_status");
    public static final Property uri = ResourceFactory.createProperty("http://purl.org/termed/properties/uri");
    public static final Property lastModifiedDate = ResourceFactory.createProperty("http://purl.org/termed/properties/lastModifiedDate");
    public static final Property createdDate = ResourceFactory.createProperty("http://purl.org/termed/properties/createdDate");
    public static final Property lastModifiedBy = ResourceFactory.createProperty("http://purl.org/termed/properties/lastModifiedBy");
    public static final Property createdBy = ResourceFactory.createProperty("http://purl.org/termed/properties/createdBy");
    public static final Property type = ResourceFactory.createProperty("http://purl.org/termed/properties/type");
    public static final Property id = ResourceFactory.createProperty("http://purl.org/termed/properties/id");
    public static final Property link = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/term#externalLink");
    public static final Property terminologyType = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/term/#terminologyType");
    public static final Property contact = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#contact");
    public static final Property conceptClass = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/iow#conceptClass");
    public static final Property subjectArea = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/term#subjectArea");
    public static final Property termFamily = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#termFamily");
    public static final Property termEquivalency = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#termEquivalency");
    public static final Property scope = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/iow#scope");
    public static final Property homographNumber = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#termHomographNumber");
    public static final Property termInfo = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#termInfo");
    public static final Property termConjugation = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#termConjugation");
    public static final Property termStyle = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#termStyle");
    public static final Property wordClass = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#termWordClass");
    public static final Property searchTerm = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#searchTerm");
    public static final Property notRecommended = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#notRecommendedSynonym");
    public static final Property targetId = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#targetId");
    public static final Property targetUri = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#targetUri");
    public static final Property synonym = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/st#synonym");
}
