package fi.vm.yti.terminology.api.v2.repository;

import fi.vm.yti.common.repository.BaseRepository;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdfconnection.RDFConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class TerminologyRepository extends BaseRepository {

    public TerminologyRepository(@Value(("${fuseki.url}")) String endpoint){
        super(RDFConnection.connect(endpoint + "/terminology/get"),
                RDFConnection.connect(endpoint + "/terminology/data"),
                RDFConnection.connect(endpoint + "/terminology/sparql"),
                RDFConnection.connect(endpoint + "/terminology/update")
        );
    }

    @Override
    public ModelWrapper fetch(String graphURI) {
        return new ModelWrapper(super.fetch(graphURI), graphURI);
    }

    public ModelWrapper fetchByPrefix(String prefix) {
        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();
        return fetch(graphURI);
    }
}
