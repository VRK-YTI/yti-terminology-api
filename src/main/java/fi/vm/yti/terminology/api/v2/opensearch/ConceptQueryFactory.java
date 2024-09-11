package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.common.opensearch.QueryFactoryUtils;
import fi.vm.yti.terminology.api.v2.service.IndexService;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Highlight;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static fi.vm.yti.common.opensearch.OpenSearchUtil.logPayload;

public class ConceptQueryFactory {
    private ConceptQueryFactory() {
        // static methods
    }

    public static SearchRequest createConceptQuery(
            ConceptSearchRequest request,
            boolean isSuperUser,
            // concept search needs a list of terminologies, since the
            // organizations themselves are not in the concept index
            Set<String> incompleteFromTerminologies) {

        var conceptQuery = getConceptBaseQuery(request, isSuperUser, incompleteFromTerminologies);

        Highlight.Builder highlight = new Highlight.Builder();
        highlight.fields("definition.*", f -> f);
        var sr = new SearchRequest.Builder()
                .index(IndexService.CONCEPT_INDEX)
                .size(QueryFactoryUtils.pageSize(request.getPageSize()))
                .from(QueryFactoryUtils.pageFrom(request.getPageFrom()))
                .sort(QueryFactoryUtils.getLangSortOptions(request.getSortLang()))
                .highlight(highlight.build())
                .query(conceptQuery)
                .build();

        logPayload(sr, IndexService.CONCEPT_INDEX);

        return sr;
    }

    private static Query getConceptBaseQuery(
            ConceptSearchRequest request,
            boolean isSuperUser,
            Set<String> incompleteFromTerminologies) {

        var allQueries = new ArrayList<Query>();

        var queryString = request.getQuery();
        if (queryString != null && !queryString.isBlank()) {
            var definitionQuery = QueryStringQuery.of(q -> q
                    .query("*" + queryString.trim() + "*")
                    .fields("label.*").boost(5.0f)
                    .fields("altLabel.*", "searchTerm.*", "hiddenTerm.*", "definition.*").boost(3.0f)
                    .fields("notRecommendedSynonym.*").boost(1.5f)
                    .fuzziness("2")
            ).toQuery();

            allQueries.add(QueryBuilders.bool()
                    .should(definitionQuery)
                    .minimumShouldMatch("1")
                    .build()
                    .toQuery());
        }

        if (request.getStatus() != null) {
            allQueries.add(TermsQuery.of(q -> q
                            .field("status")
                            .terms(t -> t.value(request
                                    .getStatus()
                                    .stream()
                                    .map(Enum::name)
                                    .map(FieldValue::of)
                                    .toList())))
                    .toQuery());
        }

        if (request.getNamespace() != null) {
            allQueries.add(TermQuery.of(q -> q
                            .field("namespace")
                            .value(FieldValue.of(request.getNamespace())))
                    .toQuery());
        }

        if (request.getExcludeNamespace() != null) {
            allQueries.add(QueryBuilders.bool()
                    .mustNot(
                        TermQuery.of(q -> q
                                .field("namespace")
                                .value(FieldValue.of(request.getExcludeNamespace())))
                        .toQuery())
                    .build().toQuery());
        }

        if (!isSuperUser) {
            // if the user is not superuser, filter out INCOMPLETE
            var draftQuery = QueryBuilders.bool()
                    .mustNot(TermsQuery.of(q -> q
                                    .field("status")
                                    .terms(t -> t.value(List.of("INCOMPLETE")
                                            .stream()
                                            .map(FieldValue::of)
                                            .toList())))
                            .toQuery())
                    .build()
                    .toQuery();
            // ...unless we were given a list of terminologies as exceptions
            if (incompleteFromTerminologies != null &&
                    !incompleteFromTerminologies.isEmpty()) {
                draftQuery = QueryBuilders.bool()
                        .should(draftQuery)
                        .should(TermsQuery.of(q -> q
                                        .field("namespace")
                                        .terms(t -> t.value(new ArrayList<>(
                                                incompleteFromTerminologies != null ?
                                                        incompleteFromTerminologies :
                                                        List.of())
                                                .stream()
                                                .map(FieldValue::of)
                                                .toList())))
                                .toQuery())
                        .build()
                        .toQuery();
            }

            allQueries.add(draftQuery);
        }

        return QueryBuilders.bool().must(allQueries).build().toQuery();
    }
}
