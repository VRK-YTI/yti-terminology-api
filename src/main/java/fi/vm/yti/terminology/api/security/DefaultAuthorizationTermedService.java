package fi.vm.yti.terminology.api.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.Identifier;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.TerminologicalVocabulary;
import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.Vocabulary;
import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToSet;
import static fi.vm.yti.terminology.api.util.CollectionUtils.requireSingle;
import static java.util.Collections.emptyList;
import static org.springframework.http.HttpMethod.GET;

@Service
public class DefaultAuthorizationTermedService implements AuthorizationTermedService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAuthorizationTermedService.class);

    private final Cache<UUID, Set<UUID>> cache;
    private final TermedRequester termedRequester;

    @Autowired
    public DefaultAuthorizationTermedService(
            TermedRequester termedRequester,
            @Value("${termed.cache.expiration:1800}") Long cacheExpireTime
    ) {
        this.termedRequester = termedRequester;

        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheExpireTime, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();
    }

    @NotNull public Set<UUID> getOrganizationIds(UUID graphId) {
        Set<UUID> uuids = null;
        try {
            uuids = cache.get(graphId, () -> fetchOrganizationIds(graphId));
        } catch (ExecutionException e) {
            LOG.error("Error fetching cached organization ids", e);
        }
        return uuids;
    }

    private Set<UUID> fetchOrganizationIds(UUID graphId) {
        List<GenericNode> result = termedRequester
                .exchange(String.format("/graphs/%s/types/%s/nodes", graphId, NodeType.TerminologicalVocabulary),
                        GET,
                        new Parameters(),
                        new ParameterizedTypeReference<>() {}
                );
        GenericNode vocabularyNode = requireSingle(result);

        return mapToSet(vocabularyNode.getReferences().getOrDefault("contributor", emptyList()), Identifier::getId);
    }
}
