package fi.vm.yti.terminology.api.v2.migration.task;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.v2.mapper.ConceptMapper;
import fi.vm.yti.terminology.api.v2.property.Term;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.UUID;

@SuppressWarnings("java:S101")
@Component
public class V3_RDFListConversion implements MigrationTask {

    private static final Logger LOG = LoggerFactory.getLogger(V3_RDFListConversion.class);
    private final TerminologyRepository repository;

    public V3_RDFListConversion(TerminologyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void migrate() {

        String allGraphsQuery = "select * where { GRAPH ?g {} }";
        var graphs = new ArrayList<String>();
        repository.querySelect(allGraphsQuery, row -> graphs.add(row.get("g").toString()));

        graphs.forEach(g -> {
            var model = repository.fetch(g);

            var orphanBlankNodes = model.listStatements()
                    .filterKeep(s -> s.getSubject().isAnon())
                    .filterKeep(s -> !model.contains(null, null, s.getSubject()))
                    .toList();

            model.remove(orphanBlankNodes);

            model.listSubjectsWithProperty(RDF.type, SKOS.Concept).forEach(concept -> {
                var properties = concept.listProperties().toList();
                properties.forEach(stmt -> handleConceptProperties(concept, stmt, model));
            });

            model.listSubjectsWithProperty(RDF.type, SKOS.Collection)
                    .forEach(V3_RDFListConversion::handleCollectionProperties);

            LOG.info("Migrated graph {}", g);
            repository.put(g, model);
        });

    }

    private static void handleCollectionProperties(Resource collection) {
        try {
            if (!collection.hasProperty(SKOS.member)) {
                return;
            }

            var members = MapperUtils.getList(collection, SKOS.member);

            if (!members.isEmpty()) {
                collection.removeAll(SKOS.member);
                members.asJavaList().forEach(member -> collection.addProperty(SKOS.member, member));

                MapperUtils.addListProperty(collection, Term.orderedMember, members.asJavaList());

                members.removeList();
            }
        } catch (Exception e) {
            LOG.warn("error converting collection {}", collection.getURI());
            LOG.error(e.getMessage(), e);
        }
    }

    private static void handleConceptProperties(Resource concept, Statement stmt, ModelWrapper model) {
        var orderProperty = ConceptMapper.orderProperties.get(stmt.getPredicate().getLocalName());

        try {
            // status is stored as a literal. change that to resource
            var status = MapperUtils.getStatus(concept);
            MapperUtils.addStatus(concept, status);

            if (ConceptMapper.termProperties.contains(stmt.getPredicate())) {

                var orderedTerms = new ArrayList<Resource>();
                var terms = stmt.getList();

                // remove property from concept
                concept.removeAll(stmt.getPredicate());

                terms.asJavaList().forEach(term -> {
                    var termResource = model.createResource(Constants.URN_UUID + UUID.randomUUID());
                    term.asResource().listProperties().forEach(s -> termResource.addProperty(s.getPredicate(), s.getObject()));
                    concept.addProperty(stmt.getPredicate(), termResource);
                    orderedTerms.add(termResource);

                    var termStatus = MapperUtils.getStatus(termResource);
                    MapperUtils.addStatus(termResource, termStatus);

                    // remove old term resources from the model
                    model.removeAll(term.asResource(), null, null);
                    model.removeAll(null, null, term.asResource());
                });

                if (orderProperty != null) {
                    MapperUtils.addListProperty(concept, orderProperty, orderedTerms);
                }

                // finally, remove the whole list object
                terms.removeList();
            } else {
                if (orderProperty == null) {
                    return;
                }

                var list = stmt.getList();
                concept.removeAll(stmt.getPredicate());

                list.asJavaList().forEach(r -> concept.addProperty(stmt.getPredicate(), r));
                MapperUtils.addListProperty(concept, orderProperty, list.asJavaList());

                // remove original list
                list.removeList();
            }
        } catch (Exception e) {
            LOG.warn("error converting concept {}, property {}", concept.getURI(), stmt.getPredicate().getURI());
        }
    }
}
