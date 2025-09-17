package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.dto.OrganizationDTO;
import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.dto.ServiceCategoryDTO;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.properties.DCAP;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.dto.TerminologyInfoDTO;
import fi.vm.yti.terminology.api.v2.opensearch.IndexTerminology;
import fi.vm.yti.terminology.api.v2.property.Term;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TerminologyMapper {

    private TerminologyMapper() {
        // only static methods
    }

    public static ModelWrapper dtoToModel(TerminologyDTO dto, String graph, List<ServiceCategoryDTO> categories, YtiUser user) {
        var model = ModelFactory.createDefaultModel();
        var resource = model.createResource(graph)
                .addProperty(RDF.type, SKOS.ConceptScheme)
                .addProperty(DCAP.preferredXMLNamespacePrefix, dto.getPrefix())
                .addProperty(DCAP.preferredXMLNamespace, graph)
                .addProperty(Term.terminologyType, dto.getGraphType().name());

        dto.getLanguages().forEach(lang -> resource.addProperty(DCTerms.language, lang));

        MapperUtils.addStatus(resource, dto.getStatus());
        MapperUtils.addCreationMetadata(resource, user);

        MapperUtils.addOptionalStringProperty(resource, SuomiMeta.contact, dto.getContact());
        MapperUtils.addLocalizedProperty(dto.getLanguages(), dto.getLabel(), resource, SKOS.prefLabel);
        MapperUtils.addLocalizedProperty(dto.getLanguages(), dto.getDescription(), resource, RDFS.comment);

        addOrganizations(dto, resource);
        addGroups(dto, categories, resource);

        return new ModelWrapper(model, graph);
    }

    public static TerminologyInfoDTO modelToDTO(ModelWrapper model,
                                                List<ServiceCategoryDTO> categories,
                                                List<OrganizationDTO> organizations,
                                                Consumer<ResourceCommonInfoDTO> userMapper) {
        var dto = new TerminologyInfoDTO();
        var resource = model.getModelResource();

        dto.setPrefix(model.getPrefix());
        dto.setUri(model.getGraphURI());
        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, SKOS.prefLabel));
        dto.setDescription(MapperUtils.localizedPropertyToMap(resource, RDFS.comment));
        dto.setContact(MapperUtils.propertyToString(resource, SuomiMeta.contact));
        dto.setGraphType(getTerminologyType(resource));
        dto.setLanguages(MapperUtils.arrayPropertyToSet(resource, DCTerms.language));
        dto.setStatus(MapperUtils.getStatus(resource));

        var groups = MapperUtils.arrayPropertyToSet(resource, DCTerms.isPartOf);

        dto.setGroups(categories.stream()
                .filter(c -> groups.contains(c.getId()))
                .collect(Collectors.toSet()));

        var contributors = MapperUtils.arrayPropertyToSet(resource, DCTerms.contributor)
                .stream()
                .map(c -> c.replace(Constants.URN_UUID, ""))
                .collect(Collectors.toSet());

        dto.setOrganizations(organizations.stream()
                .filter(o -> contributors.contains(o.getId()))
                .collect(Collectors.toSet()));

        MapperUtils.mapCreationInfo(dto, resource, userMapper);

        return dto;
    }

    public static IndexTerminology toIndexDocument(ModelWrapper model, List<ServiceCategoryDTO> categories) {
        var terminologyResource = model.getModelResource();

        var index = new IndexTerminology();
        index.setPrefix(model.getPrefix());
        index.setUri(model.getModelResource().getURI());
        index.setId(model.getGraphURI());
        index.setLanguages(MapperUtils.arrayPropertyToList(terminologyResource, DCTerms.language));
        index.setLabel(MapperUtils.localizedPropertyToMap(terminologyResource, SKOS.prefLabel));
        index.setDescription(MapperUtils.localizedPropertyToMap(terminologyResource, RDFS.comment));
        index.setStatus(MapperUtils.getStatus(terminologyResource));
        index.setModified(terminologyResource.getProperty(DCTerms.modified).getString());
        index.setCreated(terminologyResource.getProperty(DCTerms.created).getString());
        index.setType(getTerminologyType(terminologyResource));
        var organizations = MapperUtils.arrayPropertyToSet(terminologyResource, DCTerms.contributor)
                .stream()
                .map(o -> o.replace(Constants.URN_UUID, ""))
                .map(UUID::fromString)
                .toList();
        index.setOrganizations(organizations);

        var addedGroups = MapperUtils.arrayPropertyToSet(terminologyResource, DCTerms.isPartOf);

        index.setGroups(categories.stream()
                .filter(c -> addedGroups.contains(c.getId()))
                .map(ServiceCategoryDTO::getIdentifier)
                .toList());
        return index;
    }

    public static void toUpdateModel(ModelWrapper model,
                                     TerminologyDTO dto,
                                     List<ServiceCategoryDTO> categories,
                                     YtiUser user) {
        var modelResource = model.getModelResource();

        modelResource.removeAll(DCTerms.language);
        modelResource.removeAll(DCTerms.contributor);
        modelResource.removeAll(DCTerms.isPartOf);

        dto.getLanguages().forEach(lang -> modelResource.addProperty(DCTerms.language, lang));

        MapperUtils.updateLocalizedProperty(dto.getLanguages(), dto.getLabel(), modelResource, SKOS.prefLabel);
        MapperUtils.updateLocalizedProperty(dto.getLanguages(), dto.getDescription(), modelResource, RDFS.comment);
        MapperUtils.updateStringProperty(modelResource, SuomiMeta.contact, dto.getContact());
        MapperUtils.updateStringProperty(modelResource, Term.terminologyType, dto.getGraphType().name());
        MapperUtils.updateStringProperty(modelResource, SuomiMeta.publicationStatus, MapperUtils.getStatusUri(dto.getStatus()));

        addOrganizations(dto, modelResource);
        addGroups(dto, categories, modelResource);

        MapperUtils.addUpdateMetadata(modelResource, user);
    }

    public static ModelWrapper mapToCopy(ModelWrapper model, String newPrefix) {
        var newURI = TerminologyURI.Factory.createTerminologyURI(newPrefix);
        var copy = ModelFactory.createDefaultModel().add(model);

        copy.listSubjects()
                .filterDrop(RDFNode::isAnon)
                .filterKeep(subject -> subject.getNameSpace().equals(model.getGraphURI()))
                .forEach(subject -> {
                    var newSubject = TerminologyURI.Factory.createConceptURI(newPrefix, subject.getLocalName());
                    ResourceUtils.renameResource(subject, newSubject.getResourceURI());
                });

        var modelResource = copy.listSubjectsWithProperty(RDF.type, SKOS.ConceptScheme)
                .nextOptional()
                .orElseThrow(() -> new ResourceNotFoundException(newURI.getModelResourceURI()));

        // add suffix "(Copy)" to terminology's label
        var label = MapperUtils.localizedPropertyToMap(modelResource, SKOS.prefLabel);
        modelResource.removeAll(SKOS.prefLabel);
        label.forEach((lang, value) ->
                modelResource.addProperty(SKOS.prefLabel, ResourceFactory.createLangLiteral(value + " (Copy)", lang)));

        MapperUtils.updateStringProperty(modelResource, DCAP.preferredXMLNamespace, newURI.getGraphURI());
        MapperUtils.updateStringProperty(modelResource, DCAP.preferredXMLNamespacePrefix, newURI.getPrefix());
        MapperUtils.updateStringProperty(modelResource, SuomiMeta.copiedFrom, model.getGraphURI());

        return new ModelWrapper(copy, newURI.getGraphURI());
    }

    private static void addGroups(TerminologyDTO dto, List<ServiceCategoryDTO> categories, Resource resource) {
        categories.stream()
                .filter(cat -> dto.getGroups().contains(cat.getIdentifier()))
                .forEach(cat -> resource.addProperty(
                        DCTerms.isPartOf,
                        ResourceFactory.createResource(cat.getId())
                ));
    }

    private static void addOrganizations(TerminologyDTO dto, Resource resource) {
        dto.getOrganizations().forEach(org -> resource.addProperty(
                DCTerms.contributor,
                ResourceFactory.createResource(Constants.URN_UUID + org)
        ));
    }

    private static GraphType getTerminologyType(Resource resource) {
        try {
            return GraphType.valueOf(MapperUtils.propertyToString(resource, Term.terminologyType));
        } catch (IllegalArgumentException e) {
            // invalid type
        }
        return null;
    }
}
