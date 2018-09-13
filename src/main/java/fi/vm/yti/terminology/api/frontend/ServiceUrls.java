package fi.vm.yti.terminology.api.frontend;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;


    @ConfigurationProperties("services")
    @Component
    @Validated
    public class ServiceUrls {

        @NotNull
        private String codeListUrl;

        @NotNull
        private String dataModelUrl;

        @NotNull
        private String groupManagementUrl;

        public String getCodeListUrl() {
            return codeListUrl;
        }

        public void setCodeListUrl(String codeListUrl) {
            this.codeListUrl = codeListUrl;
        }

        public String getDataModelUrl() {
            return dataModelUrl;
        }

        public void setDataModelUrl(String dataModelUrl) {
            this.dataModelUrl = dataModelUrl;
        }

        public String getGroupManagementUrl() {
            return groupManagementUrl;
        }

        public void setGroupManagementUrl(String groupManagementUrl) {
            this.groupManagementUrl = groupManagementUrl;
        }
    }

