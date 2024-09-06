package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.common.opensearch.OpenSearchInitializer;
import fi.vm.yti.common.opensearch.OpenSearchUtil;
import fi.vm.yti.common.service.FrontendService;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.terminology.api.v2.mapper.ConceptMapper;
import fi.vm.yti.terminology.api.v2.mapper.TerminologyMapper;
import fi.vm.yti.terminology.api.v2.opensearch.IndexConcept;
import fi.vm.yti.terminology.api.v2.opensearch.IndexTerminology;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static fi.vm.yti.common.opensearch.OpenSearchUtil.*;

@Service
public class IndexService extends OpenSearchInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(IndexService.class);
    public static final String TERMINOLOGY_INDEX = "terminologies_v2";
    public static final String CONCEPT_INDEX = "concepts_v2";

    private final OpenSearchClientWrapper client;
    private final TerminologyRepository repository;
    private final FrontendService frontendService;

    public IndexService(OpenSearchClientWrapper client,
                        TerminologyRepository repository,
                        FrontendService frontendService) {
        super(client);
        this.client = client;
        this.repository = repository;
        this.frontendService = frontendService;
    }

    public void initIndexes() {
        InitIndexesFunction fn = this::initTerminologyIndex;

        var indexConfig = Map.of(
                TERMINOLOGY_INDEX, getTerminologyMappings(),
                // CONCEPT_INDEX, new TypeMapping.Builder().build()
                CONCEPT_INDEX, getConceptMappings()
        );
        super.initIndexes(fn, indexConfig);
    }

    public void addTerminologyToIndex(IndexTerminology indexTerminology) {
        client.putToIndex(TERMINOLOGY_INDEX, indexTerminology);
    }

    public void updateTerminologyToIndex(IndexTerminology indexTerminology) {
        client.updateToIndex(TERMINOLOGY_INDEX, indexTerminology);
    }

    public void deleteTerminologyFromIndex(String id) {
        client.removeFromIndex(TERMINOLOGY_INDEX, id);
    }

    public void addConceptToIndex(IndexConcept indexConcept) {
        client.putToIndex(CONCEPT_INDEX, indexConcept);
    }

    public void updateConceptToIndex(IndexConcept indexConcept) {
        client.updateToIndex(CONCEPT_INDEX, indexConcept);
    }

    public void deleteConceptFromIndex(String id) {
        client.removeFromIndex(CONCEPT_INDEX, id);
    }

    private void initTerminologyIndex() {
        LOG.info("Init terminologies index");

        var selectBuilder = new SelectBuilder();
        selectBuilder.addPrefixes(Constants.PREFIXES);
        var exprFactory = selectBuilder.getExprFactory();
        var expr = exprFactory.strstarts(exprFactory.str("?g"), Constants.TERMINOLOGY_NAMESPACE);
        selectBuilder.addFilter(expr);
        selectBuilder.addGraph("?g", new WhereBuilder());

        var categories = frontendService.getServiceCategories();

        repository.querySelect(selectBuilder.build(), (var row) -> {
            var graph = row.get("g").toString();
            LOG.info("Indexing terminology {} metadata", graph);

            var model = repository.fetch(graph);
            var index = TerminologyMapper.toIndexDocument(model, categories);
            client.putToIndex(TERMINOLOGY_INDEX, index);

            initConceptIndex(model);
        });
    }

    private void initConceptIndex(ModelWrapper model) {
        LOG.info("Init concepts for terminology {}", model.getGraphURI());

        var concepts = model.listSubjectsWithProperty(RDF.type, SKOS.Concept)
                .mapWith(concept -> ConceptMapper.toIndexDocument(model, concept.getLocalName()))
                .toList();
        client.bulkInsert(CONCEPT_INDEX, concepts);
    }

    private TypeMapping getTerminologyMappings() {
        return new TypeMapping.Builder()
                .dynamicTemplates(OpenSearchUtil.getMetaDataDynamicTemplates())
                .properties(OpenSearchUtil.getMetaDataProperties())
                .build();
    }

    private TypeMapping getConceptMappings() {
        return new TypeMapping.Builder()
                .dynamicTemplates(List.of(
                        getDynamicTemplate("label", "label.*"),
                        getDynamicTemplate("definition", "definition.*"),
                        getDynamicTemplate("altLabel", "altLabel.*"),
                        getDynamicTemplate("searchTerm", "searchTerm.*"),
                        getDynamicTemplate("notRecommendedSynonym", "notRecommendedSynonym.*")))
                .properties(Map.ofEntries(
                        Map.entry("id", getKeywordProperty()),
                        Map.entry("identifier", getKeywordProperty()),
                        Map.entry("uri", getKeywordProperty()),
                        Map.entry("status", getKeywordProperty()),
                        Map.entry("namespace", getKeywordProperty()),
                        Map.entry("prefix", getKeywordProperty()),
                        Map.entry("created", getDateProperty()),
                        Map.entry("modified", getDateProperty())
                ))
                .build();
    }

    public void reindexTerminology(ModelWrapper model) {
        deleteTerminologyFromIndex(model.getGraphURI());

        var categories = frontendService.getServiceCategories();

        var index = TerminologyMapper.toIndexDocument(model, categories);
        client.putToIndex(TERMINOLOGY_INDEX, index);

        initConceptIndex(model);
    }
}
