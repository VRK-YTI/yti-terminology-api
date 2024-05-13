package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.common.dto.MetaDataDTO;
import fi.vm.yti.common.enums.GraphType;

public class TerminologyDTO extends MetaDataDTO {

    private GraphType graphType;

    public GraphType getGraphType() {
        return graphType;
    }

    public void setGraphType(GraphType graphType) {
        this.graphType = graphType;
    }
}
