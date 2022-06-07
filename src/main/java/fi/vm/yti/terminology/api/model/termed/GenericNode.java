package fi.vm.yti.terminology.api.model.termed;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;

public final class GenericNode implements Node, Serializable {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID id = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String code = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String uri = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long number = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String createdBy = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Date createdDate = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String lastModifiedBy = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Date lastModifiedDate = null;

    private final TypeId type;

    private final Map<String, List<Attribute>> properties;

    private final Map<String, List<Identifier>> references;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private  Map<String, List<Identifier>> referrers = null;

    // Jackson constructor
    private GenericNode() {
        this(randomUUID(), null, null, 0L, null, new Date(), null, new Date(), TypeId.placeholder(), emptyMap(), emptyMap(), emptyMap());
    }

    public GenericNode(UUID id,
                       String code,
                       String uri,
                       Long number,
                       String createdBy,
                       Date createdDate,
                       String lastModifiedBy,
                       Date lastModifiedDate,
                       TypeId type,
                       Map<String, List<Attribute>> properties,
                       Map<String, List<Identifier>> references,
                       Map<String, List<Identifier>> referrers) {
        this.id = id;
        this.code = code;
        this.uri = uri;
        this.number = number;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.lastModifiedBy = lastModifiedBy;
        this.lastModifiedDate = lastModifiedDate;
        this.type = type;
        this.properties = properties;
        this.references = references;
        this.referrers = referrers;
    }

    /**
     * Used for import. No given UUID so random uuid is created on upon call
     * @param code
     * @param uri
     * @param number
     * @param createdBy
     * @param createdDate
     * @param lastModifiedBy
     * @param lastModifiedDate
     * @param type
     * @param properties
     * @param references
     * @param referrers
     */
    public GenericNode(String code,
                       String uri,
                       Long number,
                       String createdBy,
                       Date createdDate,
                       String lastModifiedBy,
                       Date lastModifiedDate,
                       TypeId type,
                       Map<String, List<Attribute>> properties,
                       Map<String, List<Identifier>> references,
                       Map<String, List<Identifier>> referrers) {
        this.id = randomUUID();
        this.code = code;
        this.uri = uri;
        this.number = number;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.lastModifiedBy = lastModifiedBy;
        this.lastModifiedDate = lastModifiedDate;
        this.type = type;
        this.properties = properties;
        this.references = references;
        this.referrers = referrers;
    }

    /**
     * Simplified creator
     * @param type typeId containing  vocabulary-id
     * @param properties Attributes as Property-map
     * @param references References as Identifier-map
     */
    public GenericNode(TypeId type,
                       Map<String, List<Attribute>> properties,
                       Map<String, List<Identifier>> references
                       ) {
        this.id = randomUUID();
        this.type = type;
        this.properties = properties;
        this.references = references;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getUri() {
        return uri;
    }

    public Long getNumber() {
        return number;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public TypeId getType() {
        return type;
    }

    public Identifier getIdentifier() {
        return new Identifier(this.id, this.type);
    }

    public Map<String, List<Attribute>> getProperties() {
        return properties;
    }

    public Map<String, List<Identifier>> getReferences() {
        return references;
    }

    public Map<String, List<Identifier>> getReferrers() {
        return referrers;
    }

    public GenericNode copyToGraph(UUID graphId) {

        TypeId newType = type.copyToGraph(graphId);

        return new GenericNode(id, code, uri, number, createdBy, createdDate, lastModifiedBy, lastModifiedDate, newType, properties, references, referrers);
    }

    /**
     * Copies all nodes and attributes to new graph with new ids
     *
     * @param graphId new graph id
     * @return
     */
    public GenericNode copyAllToGraph(UUID graphId, Map<UUID, UUID> nodeIdMap) {

        TypeId newType = type.copyToGraph(graphId);

        var iteratorReferences = references.keySet().iterator();
        var iteratorReferrers = referrers.keySet().iterator();

        Map<String, List<Identifier>> referencesNewGraph = new HashMap<>();
        Map<String, List<Identifier>> referrersNewGraph = new HashMap<>();

        Function<Identifier, Identifier> mapper = i -> {
            // use node's original id if not present in id map
            UUID newId = nodeIdMap.getOrDefault(i.getId(), i.getId());
            TypeId typeId;

            // use original identifier for organization and group references
            if (Arrays.asList(NodeType.Organization, NodeType.Group).contains(i.getType().getId())) {
                typeId = i.getType();
            } else {
                typeId = new TypeId(i.getType().getId(), new GraphId(graphId));
            }
            return new Identifier(newId, typeId);
        };

        while(iteratorReferences.hasNext()) {
            String key = iteratorReferences.next();

            referencesNewGraph.put(key, references.get(key)
                    .stream()
                    .map(mapper)
                    .collect(Collectors.toList())
            );
        }

        while(iteratorReferrers.hasNext()) {
            String key = iteratorReferrers.next();

            referrersNewGraph.put(key, referrers.get(key)
                    .stream()
                    .map(mapper)
                    .collect(Collectors.toList())
            );
        }

        return new GenericNode(id, code, uri, number, createdBy, createdDate, lastModifiedBy, lastModifiedDate,
                newType, properties, referencesNewGraph, referrersNewGraph);
    }
}
