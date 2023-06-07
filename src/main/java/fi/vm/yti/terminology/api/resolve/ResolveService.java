package fi.vm.yti.terminology.api.resolve;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import fi.vm.yti.terminology.api.TermedContentType;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.exception.ResourceNotFoundException;
import fi.vm.yti.terminology.api.exception.VocabularyNotFoundException;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.Graph;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.resolve.ResolvedResource.Type;
import fi.vm.yti.terminology.api.util.Parameters;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;

@Service
public class ResolveService {

    private static final Logger logger = LoggerFactory.getLogger(ResolveService.class);
    private final TermedRequester termedRequester;
    private final String namespaceRoot;

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^(?<prefix>[\\w\\-]+)/?$");
    private static final Pattern PREFIX_AND_RESOURCE_PATTERN = Pattern.compile("^(?<prefix>[\\w\\-]+)/(?<resource>[\\w\\-]+)$");

    @Autowired
    ResolveService(TermedRequester termedRequester,
                   @Value("${namespace.root}") String namespaceRoot) {
        this.termedRequester = termedRequester;
        this.namespaceRoot = namespaceRoot;
    }

    public ResolvedResource resolveResource(String uri) {

        if (!uri.startsWith(namespaceRoot)) {
            throw new ResolveException("Unsupported URI namespace: " + uri);
        }

        var uriWithoutParameters = uri.replaceFirst("\\?.*$", "");
        var path = uriWithoutParameters.substring(namespaceRoot.length());

        var prefixMatcher = PREFIX_PATTERN.matcher(path);
        if (prefixMatcher.matches()) {
            var prefix = prefixMatcher.group("prefix");
            var graphId = findGraphIdForPrefix(prefix);
            return new ResolvedResource(graphId, Type.VOCABULARY);
        }

        var prefixAndResourceMatcher = PREFIX_AND_RESOURCE_PATTERN.matcher(path);
        if (prefixAndResourceMatcher.matches()) {
            var prefix = prefixAndResourceMatcher.group("prefix");
            var resource = prefixAndResourceMatcher.group("resource");

            var graphId = findGraphIdForPrefix(prefix);
            var nodes = findNodes(graphId, resource);
            if (nodes.size() > 1) {
                logger.debug("Found {} matching nodes for URI: {}", nodes.size(), uri);
            }
            for (GenericNode node : nodes) {
                switch (node.getType().getId()) {
                    case TerminologicalVocabulary:
                        return new ResolvedResource(graphId, Type.VOCABULARY);
                    case Concept:
                        return new ResolvedResource(graphId, Type.CONCEPT, node.getId());
                    case Collection:
                        return new ResolvedResource(graphId, Type.COLLECTION, node.getId());
                    default:
                        logger.debug("Found node of type {} for URI: {}", node.getType().getId(), uri);
                }
            }

            logger.error("Resource not found URI: {}", uri);
            throw new ResourceNotFoundException(prefix, resource);
        }

        throw new ResolveException("Unsupported URI: " + uri);
    }

    private List<GenericNode> findNodes(UUID graphId,
                                        String code) {
        var params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "code:" + code);
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange(TermedRequester.PATH_NODE_TREES, GET, params, new ParameterizedTypeReference<List<GenericNode>>() {}));

    }

    // FIXME inefficient implementation but termed doesn't provide better way (afaik)
    private @NotNull UUID findGraphIdForPrefix(String prefix) {

        var params = new Parameters();
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange(TermedRequester.PATH_GRAPHS, GET, params, new ParameterizedTypeReference<List<Graph>>() {
        }))
            .stream()
            .filter(g -> g.getCode().equalsIgnoreCase(prefix))
            .findFirst()
            .orElseThrow(() -> new VocabularyNotFoundException(prefix))
            .getId();
    }

    String getResource(@NotNull UUID graphId,
                       @NotNull List<NodeType> types,
                       TermedContentType contentType,
                       @Nullable UUID resourceId) {

        var params = new Parameters();
        params.add("select", "*");
        params.add("select", "properties.*");
        params.add("select", "references.*");
        params.add("where", formatWhereClause(graphId, types, resourceId));
        params.add("pretty", "true");

        return requireNonNull(termedRequester.exchange(TermedRequester.PATH_NODE_TREES, GET, params, String.class, contentType));
    }

    String getTerminology(@NotNull UUID id, TermedContentType contentType) {
        var params = new Parameters();
        params.add("select", "*");
        //params.add("select", "references.prefLabelXl:1");
        params.add("max", "-1");
        params.add("pretty", "true");
        return requireNonNull(termedRequester.exchange("/graphs/" + id + TermedRequester.PATH_NODE_TREES, GET, params, String.class, contentType));
    }

    private static @NotNull String formatWhereClause(@NotNull UUID graphId,
                                                     @NotNull List<NodeType> types,
                                                     @Nullable UUID resourceId) {

        if (types.isEmpty()) {
            throw new IllegalArgumentException("Must include at least one type");
        }

        String typeClause = types.stream().map(t -> "type.id:" + t.name()).collect(Collectors.joining(" OR "));

        return "graph.id:" + graphId +
            " AND (" + typeClause + ")" + (resourceId != null ? " AND id:" + resourceId : "");
    }

    public static class ResolveException extends RuntimeException {

        ResolveException(String message) {
            super(message);
            logger.error(message);
        }
    }
}
