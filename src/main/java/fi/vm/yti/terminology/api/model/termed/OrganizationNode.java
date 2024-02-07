package fi.vm.yti.terminology.api.model.termed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.vm.yti.terminology.api.synchronization.GroupManagementOrganization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static fi.vm.yti.terminology.api.model.termed.NodeType.Organization;
import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;

@JsonIgnoreProperties(value = { "referrers", "number", "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate" })
public final class OrganizationNode implements Node {

    private static final String DEFAULT_ATTRIBUTE_REGEX = "(?s)^.*$";

    private final UUID id;
    private final TypeId type;
    private final Map<String, List<Attribute>> properties;
    private final Map<String, List<Identifier>> references;

    // Jackson constructor
    private OrganizationNode() {
        this(randomUUID(), TypeId.placeholder(), emptyMap(), new ArrayList<>());
    }

    public OrganizationNode(UUID id, TypeId type, Map<String, String> prefLabel, List<Identifier> parent) {
        this.id = id;
        this.type = type;
        this.properties = singletonMap("prefLabel", localizableToLocalizations(prefLabel));
        this.references = singletonMap("parent", parent);
    }

    public static OrganizationNode fromGroupManagement(GroupManagementOrganization org, String organizationGraphId) {
        TypeId type = new TypeId(Organization, new GraphId(UUID.fromString(organizationGraphId)));

        List<Identifier> parent = org.getParentId() != null
                ? List.of(new Identifier(org.getParentId(), type))
                : new ArrayList<>();

        return new OrganizationNode(org.getUuid(), type, org.getPrefLabel(), parent);
    }

    private static List<Attribute> localizableToLocalizations(Map<String, String> localizable) {
        return mapToList(localizable.entrySet(), entry -> {
            String lang = entry.getKey();
            String value = entry.getValue();
            return new Attribute(lang, value, DEFAULT_ATTRIBUTE_REGEX);
        });
    }

    public Identifier getIdentifier() {
        return new Identifier(this.id, this.type);
    }

    public UUID getId() {
        return id;
    }

    @Override
    public TypeId getType() {
        return type;
    }

    @Override
    public Map<String, List<Attribute>> getProperties() {
        return properties;
    }

    public Map<String, List<Identifier>> getReferences() {
        return references;
    }
}
