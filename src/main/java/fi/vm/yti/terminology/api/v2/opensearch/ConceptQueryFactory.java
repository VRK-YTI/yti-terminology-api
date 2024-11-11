package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.common.enums.Status;
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
        var builder = new SearchRequest.Builder()
                .index(IndexService.CONCEPT_INDEX)
                .size(QueryFactoryUtils.pageSize(request.getPageSize()))
                .from(QueryFactoryUtils.pageFrom(request.getPageFrom()))
                .highlight(highlight.build())
                .query(conceptQuery);

        // use score based sorting if query string specified
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            builder.sort(QueryFactoryUtils.getLangSortOptions(request.getSortLang()));
        }

        var sr = builder.build();
        logPayload(sr, IndexService.CONCEPT_INDEX);

        return sr;
    }

    public static Query getConceptBaseQuery(
            ConceptSearchRequest request,
            boolean isSuperUser,
            Set<String> incompleteFromTerminologies) {

        var allQueries = new ArrayList<Query>();

        var queryString = request.getQuery();
        if (queryString != null && !queryString.isBlank()) {
            var qs = queryString.trim();

            // If there are spaces in the query, add quotations to search with an exact phrase.
            // In case of a single word, search exact match (with fuzzy) or with wild cards. Exact match will be ranked higher.
            final var query = qs.contains(" ")
                    ? String.format("\"%s\"", qs)
                    : String.format("%s~1 *%s*", qs, qs);

            var definitionQuery = QueryStringQuery.of(q -> q
                    .query(query)
                    .fields("label.*^5.0")
                    .fields("altLabel^3.0", "searchTerm^3.0", "definition.*^3.0")
                    .fields("notRecommendedSynonym")
            ).toQuery();

            allQueries.add(definitionQuery);
        }

        // By default, search only for VALID, DRAFT and INCOMPLETE concepts
        var statusList = request.getStatus() != null && !request.getStatus().isEmpty()
                ? request.getStatus()
                : Set.of(Status.DRAFT, Status.INCOMPLETE, Status.VALID);

        allQueries.add(TermsQuery.of(q -> q
                        .field("status")
                        .terms(t -> t.value(statusList
                                .stream()
                                .map(Enum::name)
                                .map(FieldValue::of)
                                .toList())))
                .toQuery());


        if (request.getNamespace() != null) {
            allQueries.add(TermQuery.of(q -> q
                            .field("namespace")
                            .value(FieldValue.of(request.getNamespace())))
                    .toQuery());
        } else if (!request.getNamespaces().isEmpty()) {
            allQueries.add(TermsQuery.of(q -> q
                            .field("namespace")
                            .terms(t -> t.value(request.getNamespaces()
                                    .stream()
                                    .map(FieldValue::of)
                                    .toList())))
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
