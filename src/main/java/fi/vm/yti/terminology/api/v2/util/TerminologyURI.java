package fi.vm.yti.terminology.api.v2.util;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.util.GraphURI;

import java.util.regex.Pattern;

public class TerminologyURI extends GraphURI {

    private static final Pattern iriPattern = Pattern.compile("https?://iri.suomi.fi/terminology/" +
                                                              "(?<prefix>[\\w-]+)/" +
                                                              "(?<resource>[\\w-]+)?");

    public static TerminologyURI createTerminologyURI(String prefix) {
        return new TerminologyURI(prefix);
    }

    public static TerminologyURI createConceptURI(String prefix, String conceptId) {
        return new TerminologyURI(prefix, conceptId);
    }

    public static TerminologyURI createConceptCollectionURI(String prefix, String conceptCollectionId) {
        return new TerminologyURI(prefix, conceptCollectionId);
    }

    public static TerminologyURI fromUri(String uri) {
        var matcher = iriPattern.matcher(uri);
        if (matcher.matches()) {
            var prefix = matcher.group("prefix");
            var resourceId = matcher.group("resource");

            if (resourceId != null) {
                return new TerminologyURI(prefix, resourceId);
            }
            return new TerminologyURI(prefix);
        }
        return new TerminologyURI(uri);
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
