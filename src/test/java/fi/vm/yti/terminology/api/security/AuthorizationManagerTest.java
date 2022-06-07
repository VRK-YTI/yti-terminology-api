package fi.vm.yti.terminology.api.security;

import fi.vm.yti.security.AuthenticatedUserProvider;

import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.GraphId;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.TypeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        AuthorizationManager.class
})
public class AuthorizationManagerTest {

    @MockBean
    AuthenticatedUserProvider userProvider;

    @MockBean
    AuthorizationTermedService termedService;

    @Autowired
    AuthorizationManager authorizationManager;

    @Test
    public void testCanModifyNodes() {
        var orgId = randomUUID();

        var graphId = randomUUID();
        var nodeType = new TypeId(NodeType.Concept, new GraphId(graphId));
        var node = new GenericNode(randomUUID(), null, null, 0L, null, new Date(),
                null, new Date(), nodeType, emptyMap(), emptyMap(), emptyMap());

        when(userProvider.getUser()).thenReturn(getUser(orgId, false));
        when(termedService.getOrganizationIds(graphId)).thenReturn(Set.of(orgId));

        assertTrue(authorizationManager.canModifyNodes(Arrays.asList(node)));
    }

    @Test
    public void testUserIsPartOfOrganization() {
        var orgId = randomUUID();
        var graphId = randomUUID();

        when(userProvider.getUser()).thenReturn(getUser(orgId, false));
        when(termedService.getOrganizationIds(graphId)).thenReturn(Set.of(orgId));

        assertTrue(authorizationManager.isUserPartOfOrganization(graphId));
    }

    @Test
    public void testUserIsNotPartOfOrganization() {
        var userOrg = randomUUID();
        var vocabularyOrg = randomUUID();
        var graphId = randomUUID();

        when(userProvider.getUser()).thenReturn(getUser(userOrg, false));
        when(termedService.getOrganizationIds(graphId)).thenReturn(Set.of(vocabularyOrg));

        assertFalse(authorizationManager.isUserPartOfOrganization(graphId));
    }

    @Test
    public void testSuperUserIsPartOfOrganization() {
        when(userProvider.getUser()).thenReturn(getUser(randomUUID(), true));
        assertTrue(authorizationManager.isUserPartOfOrganization(randomUUID()));
    }

    private static YtiUser getUser(UUID orgId, boolean isSuperUser) {
        Map<UUID, Set<Role>> rolesMap = new HashMap<>();
        rolesMap.put(orgId, Set.of(Role.ADMIN));
        return new YtiUser("test@test.fi",
                "Test", "User",
                randomUUID(),
                isSuperUser, false,
                null, null, rolesMap, null, null);
    }
}
