package fi.vm.yti.terminology.api.v2.security;

import fi.vm.yti.common.security.BaseAuthorizationManagerImpl;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;

@Service
public class TerminologyAuthorizationManager extends BaseAuthorizationManagerImpl {

    public TerminologyAuthorizationManager(AuthenticatedUserProvider userProvider) {
        super(userProvider);
    }

    public boolean hasRightToAnyOrganization(Collection<UUID> organizations) {
        if (organizations.isEmpty()) {
            return false;
        }
        return hasRightToAnyOrganization(organizations, Role.TERMINOLOGY_EDITOR);
    }

    public boolean hasRightsToTerminology(String prefix, Model model) {
        var graphURI = TerminologyURI.createTerminologyURI(prefix).getGraphURI();
        return hasRightToModel(graphURI, model, Role.TERMINOLOGY_EDITOR);
    }
}
