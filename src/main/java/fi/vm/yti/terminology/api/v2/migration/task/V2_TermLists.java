package fi.vm.yti.terminology.api.v2.migration.task;

import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.v2.mapper.ConceptMapper;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@SuppressWarnings("java:S101")
@Component
public class V2_TermLists implements MigrationTask {

    private final TerminologyRepository repository;

    public V2_TermLists(TerminologyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void migrate() {

        String allGraphsQuery = "select * where { GRAPH ?g {} }";
        var graphs = new ArrayList<String>();
        repository.querySelect(allGraphsQuery, row -> graphs.add(row.get("g").toString()));

        // move terms to RDF lists
        graphs.forEach(g -> {
            var model = repository.fetch(g);

            ConceptMapper.termProperties.forEach(property ->
                model.listSubjectsWithProperty(property).forEach(s -> {
                    if (MapperUtils.hasType(s, SKOS.Concept)) {
                        var terms = MapperUtils.arrayPropertyToList(s, property);

                        var termResources = terms.stream().map(t -> {
                            var termRes = model.getResource(t);
                            var anonRes = model.createResource();

                            termRes.listProperties().forEach(p -> anonRes.addProperty(p.getPredicate(), p.getObject()));
                            model.removeAll(termRes, null, null);

                            return anonRes;
                        });
                        s.removeAll(property);
                        s.addProperty(property, model.createList(termResources.iterator()));
                    }
                })
            );
            repository.put(g, model);
        });
    }
}
