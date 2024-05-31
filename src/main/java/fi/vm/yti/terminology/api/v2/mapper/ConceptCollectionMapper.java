package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionInfoDTO;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

import java.util.List;
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
                .addProperty(SKOS.inScheme,
                        ResourceFactory.createResource(modelResource.getURI()))
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

        var memberIdentifiers = dto.getMembers().stream().toList();
        var conceptURIs = memberIdentifiers.stream()
                .map(member -> modelResource.getNameSpace() + member)
                .toList();
        addListProperty(
                conceptCollectionResource,
                SKOS.member,
                conceptURIs);

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

        conceptCollectionResource.removeAll(SKOS.member);
        dto.getMembers().stream().forEach(conceptIdentifier -> {
            var conceptResource = model.getResourceById(conceptIdentifier);
            conceptCollectionResource.addProperty(SKOS.member, conceptResource);
        });

        MapperUtils.addUpdateMetadata(conceptCollectionResource, user);
    }

    public static ConceptCollectionInfoDTO modelToDTO(
            ModelWrapper model,
            String conceptCollectionIdentifier,
            Consumer<ResourceCommonInfoDTO> mapUser) {
        var resource = model.getResourceById(conceptCollectionIdentifier);
        var dto = new ConceptCollectionInfoDTO();

        dto.setIdentifier(resource.getLocalName());
        dto.setUri(resource.getURI());

        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, SKOS.prefLabel));
        dto.setDescription(MapperUtils.localizedPropertyToMap(resource, RDFS.comment));

        MapperUtils.arrayPropertyToSet(
                        resource,
                        SKOS.member)
                .stream()
                .forEach((conceptUri) -> {
                    var concept = model.getResourceById(
                            conceptUri.substring(
                                    conceptUri.lastIndexOf("/") + 1));
                    var definition = MapperUtils.localizedPropertyToMap(concept, SKOS.definition);
                    dto.addMember(
                            concept.getLocalName(),
                            definition);
                });

        MapperUtils.mapCreationInfo(dto, resource, mapUser);

        return dto;
    }

    public static void mapDeleteConceptCollection(ModelWrapper model, String identifier) {
        var resource = model.getResourceById(identifier);

        model.removeAll(null, null, resource);
        model.removeAll(resource, null, null);
    }

    private static void addListProperty(Resource resource, Property property, List<String> values) {
        resource.removeAll(property);
        if (values.isEmpty()) {
            return;
        }
        var list = resource.getModel().createList(values.stream()
                .map(ResourceFactory::createStringLiteral)
                .iterator());
        resource.addProperty(property, list);
    }
}
