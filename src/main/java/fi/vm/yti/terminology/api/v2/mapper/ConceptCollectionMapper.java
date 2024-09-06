package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionInfoDTO;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.*;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;

public class ConceptCollectionMapper {

    private ConceptCollectionMapper() {
    }

    public static void dtoToModel(
            ModelWrapper model,
            ConceptCollectionDTO dto,
            YtiUser user) {
        var modelResource = model.getModelResource();
        var conceptCollectionResource = model.createResourceWithId(dto.getIdentifier())
                .addProperty(SKOS.inScheme, modelResource)
                .addProperty(RDF.type, SKOS.Collection);

        var languages = MapperUtils.arrayPropertyToSet(
                modelResource,
                DCTerms.language);

        MapperUtils.addLocalizedProperty(
                languages,
                dto.getLabel(),
                conceptCollectionResource,
                SKOS.prefLabel);
        MapperUtils.addLocalizedProperty(
                languages,
                dto.getDescription(),
                conceptCollectionResource,
                RDFS.comment);

        addMembers(
                model,
                conceptCollectionResource,
                dto.getMembers());

        MapperUtils.addCreationMetadata(
                conceptCollectionResource,
                user);
    }

    public static void dtoToUpdateModel(
            ModelWrapper model,
            String conceptCollectionIdentifier,
            ConceptCollectionDTO dto,
            YtiUser user) {
        var conceptCollectionResource = model.getResourceById(conceptCollectionIdentifier);

        var languages = MapperUtils.arrayPropertyToSet(
                model.getModelResource(),
                DCTerms.language);
        MapperUtils.updateLocalizedProperty(
                languages,
                dto.getLabel(),
                conceptCollectionResource,
                SKOS.prefLabel);
        MapperUtils.updateLocalizedProperty(
                languages,
                dto.getDescription(),
                conceptCollectionResource,
                RDFS.comment);

        addMembers(
                model,
                conceptCollectionResource,
                dto.getMembers());

        MapperUtils.addUpdateMetadata(conceptCollectionResource, user);
    }

    public static ConceptCollectionInfoDTO modelToDTO(
            ModelWrapper model,
            String conceptCollectionIdentifier,
            Consumer<ResourceCommonInfoDTO> mapUser) {
        var resource = model.getResourceById(conceptCollectionIdentifier);

        if (!MapperUtils.hasType(resource, SKOS.Collection)
            || !model.contains(resource, null)) {
            throw new ResourceNotFoundException(conceptCollectionIdentifier);
        }
        var dto = new ConceptCollectionInfoDTO();

        dto.setIdentifier(resource.getLocalName());
        dto.setUri(resource.getURI());

        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, SKOS.prefLabel));
        dto.setDescription(MapperUtils.localizedPropertyToMap(resource, RDFS.comment));

        MapperUtils.getResourceList(resource, SKOS.member)
                .forEach(concept -> {
                    var labelMap = new HashMap<String, String>();
                    MapperUtils.getResourceList(concept, SKOS.prefLabel).forEach(term -> {
                        if (term.hasProperty(SKOSXL.literalForm)) {
                            var labelProperty = term.getProperty(SKOSXL.literalForm);
                            labelMap.put(labelProperty.getLanguage(), labelProperty.getString());
                        }
                    });
                    dto.addMember(concept.getLocalName(), concept.getURI(), labelMap, model.getPrefix());
                });

        MapperUtils.mapCreationInfo(dto, resource, mapUser);

        return dto;
    }

    public static void mapDeleteConceptCollection(ModelWrapper model, String identifier) {
        var resource = model.getResourceById(identifier);

        model.removeAll(null, null, resource);
        model.removeAll(resource, null, null);
    }

    private static void addMembers(ModelWrapper model, Resource resource, Set<String> values) {
        var resources = values.stream()
                .map(model::getResourceById)
                .toList();

        MapperUtils.addListProperty(resource, SKOS.member, resources);
    }
}
