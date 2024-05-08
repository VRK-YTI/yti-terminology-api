package fi.vm.yti.terminology.api.v2.repository;

import fi.vm.yti.common.Constants;
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
        var model = new ModelWrapper(super.fetch(graphURI), graphURI);
        model.setNsPrefixes(Constants.PREFIXES);
        model.setNsPrefix("term", "https://iri.suomi.fi/model/term/");
        model.setNsPrefix("skos-xl", "http://www.w3.org/2008/05/skos-xl#");
        model.setNsPrefix(model.getPrefix(), model.getModelResource().getNameSpace());
        return model;
    }

    public ModelWrapper fetchByPrefix(String prefix) {
        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();
        return fetch(graphURI);
    }
}
