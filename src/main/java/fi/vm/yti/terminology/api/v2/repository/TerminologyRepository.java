package fi.vm.yti.terminology.api.v2.repository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.yti.common.Constants;
import fi.vm.yti.common.repository.BaseRepository;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.terminology.api.v2.property.Term;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class TerminologyRepository extends BaseRepository {

    private final Cache<String, ModelWrapper> modelCache;

    public TerminologyRepository(@Value(("${fuseki.url}")) String endpoint){
        super(RDFConnection.connect(endpoint + "/terminology/get"),
                RDFConnection.connect(endpoint + "/terminology/data"),
                RDFConnection.connect(endpoint + "/terminology/sparql"),
                RDFConnection.connect(endpoint + "/terminology/update")
        );
        this.modelCache = CacheBuilder.newBuilder().maximumSize(50).build();
    }

    @Override
    public ModelWrapper fetch(String graphURI) {
        var model = modelCache.getIfPresent(graphURI);

        if (model != null) {
            return model;
        }
        model = new ModelWrapper(super.fetch(graphURI), graphURI);
        model.setNsPrefixes(Constants.PREFIXES);
        model.setNsPrefix("term", Term.getNamespace());
        model.setNsPrefix(model.getPrefix(), model.getModelResource().getNameSpace());

        modelCache.put(graphURI, model);
        return model;
    }

    @Override
    public void put(String graph, Model model) {
        super.put(graph, model);
        modelCache.invalidate(graph);
    }

    public ModelWrapper fetchByPrefix(String prefix) {
        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();
        return fetch(graphURI);
    }
}
