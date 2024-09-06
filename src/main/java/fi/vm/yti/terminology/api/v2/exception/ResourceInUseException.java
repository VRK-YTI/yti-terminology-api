package fi.vm.yti.terminology.api.v2.exception;

import java.util.List;
import java.util.Map;

public class ResourceInUseException extends RuntimeException {

    private final List<ReferenceDetail> refList;

    public ResourceInUseException(List<ReferenceDetail> refList) {
        super("resource-in-use");
        this.refList = refList;
    }

    public List<ReferenceDetail> getRefList() {
        return refList;
    }

    public static class ReferenceDetail {
        private Map<String, String> label;
        private String uri;
        private String property;

        public Map<String, String> getLabel() {
            return label;
        }

        public void setLabel(Map<String, String> label) {
            this.label = label;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }
    }
}


