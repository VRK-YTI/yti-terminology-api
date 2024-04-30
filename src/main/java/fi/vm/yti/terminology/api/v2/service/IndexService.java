package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.common.opensearch.OpenSearchInitializer;
import fi.vm.yti.common.opensearch.OpenSearchUtil;
import fi.vm.yti.common.service.FrontendService;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.terminology.api.v2.mapper.TerminologyMapper;
import fi.vm.yti.terminology.api.v2.opensearch.IndexTerminology;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

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
                CONCEPT_INDEX, new TypeMapping.Builder().build()
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
    }

    private TypeMapping getTerminologyMappings() {
        return new TypeMapping.Builder()
                .dynamicTemplates(OpenSearchUtil.getMetaDataDynamicTemplates())
                .properties(OpenSearchUtil.getMetaDataProperties())
                .build();
    }
}
