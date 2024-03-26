package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.common.opensearch.OpenSearchInitializer;
import fi.vm.yti.terminology.api.v2.opensearch.IndexTerminology;
import org.opensearch.client.opensearch._types.mapping.*;
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

    private void initTerminologyIndex() {
        LOG.info("Init terminologies");
    }

    private void initConceptIndex() {
        LOG.info("Init concepts");
    }

    private TypeMapping getTerminologyMappings() {
        return new TypeMapping.Builder()
                .dynamicTemplates(getTerminologyDynamicTemplates())
                .properties(getTerminologyProperties())
                .build();
    }

    private List<Map<String, DynamicTemplate>> getTerminologyDynamicTemplates() {
        return List.of(
                getDynamicTemplate("label", "label.*"),
                getDynamicTemplate("description", "description.*")
        );
    }

    private Map<String, Property> getTerminologyProperties() {
        return Map.ofEntries(
                Map.entry("id", getKeywordProperty()),
                Map.entry("status", getKeywordProperty()),
                Map.entry("type", getKeywordProperty()),
                Map.entry("prefix", getKeywordProperty()),
                Map.entry("contributor", getKeywordProperty()),
                Map.entry("language", getKeywordProperty()),
                Map.entry("uri", getKeywordProperty()),
                Map.entry("created", getDateProperty()),
                Map.entry("contentModified", getDateProperty())
        );
    }


}
