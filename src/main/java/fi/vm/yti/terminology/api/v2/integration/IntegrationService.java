package fi.vm.yti.terminology.api.v2.integration;

import fi.vm.yti.common.opensearch.IndexBase;
import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.common.opensearch.QueryFactoryUtils;
import fi.vm.yti.common.opensearch.SearchResponseDTO;
import fi.vm.yti.terminology.api.v2.opensearch.IndexConcept;
import fi.vm.yti.terminology.api.v2.opensearch.IndexTerminology;
import fi.vm.yti.terminology.api.v2.service.IndexService;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
public class IntegrationService {

    private final OpenSearchClientWrapper client;

    public IntegrationService(OpenSearchClientWrapper client) {
        this.client = client;
    }

    public IntegrationResponse getContainers(Set<String> uris) {
        var request = new SearchRequest.Builder();

        request.size(1000);
        request.index(IndexService.TERMINOLOGY_INDEX);

        if (!uris.isEmpty()) {
            request.query(QueryFactoryUtils.termsQuery("uri", fixURIs(uris)));
        }

        var result = client.search(request.build(), IndexTerminology.class);

        var response = new IntegrationResponse();
        response.setMeta(getMeta(result));
        response.setResults(getResults(result.getResponseObjects(), "terminology"));

        return response;
    }

    public IntegrationResponse getContainerResources(IntegrationController.ResourceRequest req) {
        var request = new SearchRequest.Builder();

        request.size(req.getPageSize() != null ? req.getPageSize() : 20);
        request.index(IndexService.CONCEPT_INDEX);

        var queries = new ArrayList<Query>();

        if (!req.getContainer().isEmpty()) {
            queries.add(
                    QueryFactoryUtils.termsQuery("namespace", fixURIs(req.getContainer()))
            );
        }

        if (req.getAfter() != null) {
            queries.add(new RangeQuery.Builder()
                    .field("modified")
                    .gte(JsonData.of(req.getAfter()))
                    .build()
                    .toQuery());
        }
        if (req.getSearchTerm() != null && !req.getSearchTerm().isBlank()) {
            queries.add(QueryFactoryUtils.labelQuery(req.getSearchTerm()));
        }
        if (req.getStatus() != null) {
            queries.add(QueryFactoryUtils.termQuery("status", req.getStatus().name()));
        }

        request.query(QueryBuilders
                .bool()
                .must(queries)
                .build()
                .toQuery());

        var result = client.search(request.build(), IndexConcept.class);

        var response = new IntegrationResponse();
        response.setMeta(getMeta(result));
        response.setResults(getResults(result.getResponseObjects(), "concept"));
        return response;
    }

    private static List<IntegrationResult> getResults(List<? extends IndexBase> responseObjects, String type) {
        return responseObjects.stream().map(index -> {
            var container = new IntegrationResult();
            container.setUri(index.getUri());
            container.setType(type);
            container.setPrefLabel(index.getLabel());

            if (index instanceof IndexConcept concept) {
                container.setDescription(concept.getDefinition());
            } else {
                container.setDescription(index.getDescription());
            }
            container.setCreated(index.getCreated());
            container.setModified(index.getModified());
            container.setStatus(index.getStatus());
            return container;
        }).toList();
    }

    private static Meta getMeta(SearchResponseDTO<? extends IndexBase> result) {
        var meta = new Meta();
        meta.setTotalResults(result.getTotalHitCount());
        meta.setResultCount(result.getResponseObjects().size());
        return meta;
    }

    private static Collection<String> fixURIs(Collection<String> uris) {
        return uris.stream()
            .map(u -> {
                u = u
                        .replace("http://uri.suomi.fi", "https://iri.suomi.fi")
                        .replaceAll("terminological-vocabulary-\\d$", "");
                if (!u.endsWith("/")) {
                    u = u + "/";
                }
                return u;
            })
            .toList();
    }
}
