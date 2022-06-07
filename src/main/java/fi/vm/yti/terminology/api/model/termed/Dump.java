package fi.vm.yti.terminology.api.model.termed;

import java.util.List;

import static java.util.Collections.*;

public class Dump {
    private List<Graph> graphs;
    private List<MetaNode> types;
    private List<GenericNode> nodes;

    public Dump() {
        this(emptyList(), emptyList(), emptyList());
    }

    public Dump(List<Graph> graphs, List<MetaNode> types, List<GenericNode> nodes) {
        this.graphs = graphs;
        this.types = types;
        this.nodes = nodes;
    }

    public List<Graph> getGraphs() {
        return graphs;
    }

    public void setGraphs(List<Graph> graphs) {
        this.graphs = graphs;
    }

    public List<MetaNode> getTypes() {
        return types;
    }

    public void setTypes(List<MetaNode> types) {
        this.types = types;
    }

    public List<GenericNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<GenericNode> nodes) {
        this.nodes = nodes;
    }
}
