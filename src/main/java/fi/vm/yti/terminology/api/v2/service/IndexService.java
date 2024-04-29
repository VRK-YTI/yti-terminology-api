package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.common.opensearch.OpenSearchInitializer;
import fi.vm.yti.common.opensearch.OpenSearchUtil;
import fi.vm.yti.terminology.api.v2.opensearch.IndexTerminology;
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

    public IndexService(OpenSearchClientWrapper client) {
        super(client);
        this.client = client;
    }

    public void initIndexes() {
        InitIndexesFunction fn = () -> {
            initTerminologyIndex();
            initConceptIndex();
        };

        var indexConfig = Map.of(
                TERMINOLOGY_INDEX, getTerminologyMappings(),
                CONCEPT_INDEX, new TypeMapping.Builder().build()
        );
        super.initIndexes(fn, indexConfig);
    }

    public void addTerminologyToIndex(IndexTerminology indexTerminology) {
        client.putToIndex(TERMINOLOGY_INDEX, indexTerminology.getId(), indexTerminology);
    }

    public void updateTerminologyToIndex(IndexTerminology indexTerminology) {
        client.updateToIndex(TERMINOLOGY_INDEX, indexTerminology.getId(), indexTerminology);
    }

    public void deleteTerminologyFromIndex(String id) {
        client.removeFromIndex(TERMINOLOGY_INDEX, id);
    }

    private void initTerminologyIndex() {
        LOG.info("Init terminologies");
    }

    private void initConceptIndex() {
        LOG.info("Init concepts");
    }

    private TypeMapping getTerminologyMappings() {
        return new TypeMapping.Builder()
                .dynamicTemplates(OpenSearchUtil.getMetaDataDynamicTemplates())
                .properties(OpenSearchUtil.getMetaDataProperties())
                .build();
    }
}
