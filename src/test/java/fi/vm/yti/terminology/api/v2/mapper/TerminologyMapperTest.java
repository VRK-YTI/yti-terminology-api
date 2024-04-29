package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminologyMapperTest {

    private static final String groupId_P10 = "http://urn.fi/URN:NBN:fi:au:ptvl:v1096";
    private static final String groupId_P11 = "http://urn.fi/URN:NBN:fi:au:ptvl:v1097";

    @Test
    void mapDtoToModel() {
        var dto = new TerminologyDTO();
        var uri = TerminologyURI.createTerminologyURI("test");
        dto.setPrefix(uri.getPrefix());
        dto.setGraphType(GraphType.TERMINOLOGICAL_VOCABULARY);
        dto.setLabel(Map.of("en", "Test terminology"));
        dto.setDescription(Map.of("en", "Test terminology description"));
        dto.setOrganizations(Set.of(TestUtils.organizationId));
        dto.setGroups(Set.of("P10"));
        dto.setContact("yhteentoimivuus@dvv.fi");
        dto.setLanguages(Set.of("en", "fi"));

        var model = TerminologyMapper.dtoToModel(dto, uri.getGraphURI(), TestUtils.categoryDTOs, TestUtils.mockUser);

        var resource = model.getModelResource();

        var group = resource.getProperty(DCTerms.isPartOf).getObject();
        var organization = resource.getProperty(DCTerms.contributor).getObject();

        assertEquals("test", model.getPrefix());
        assertEquals(SKOS.ConceptScheme, resource.getProperty(RDF.type).getObject());
        assertEquals("Test terminology", MapperUtils.localizedPropertyToMap(resource, SKOS.prefLabel).get("en"));
        assertEquals("Test terminology description", MapperUtils.localizedPropertyToMap(resource, RDFS.comment).get("en"));
        assertTrue(group.isResource());
        assertTrue(organization.isResource());
        assertEquals(Constants.URN_UUID + TestUtils.organizationId, organization.toString());
        assertEquals(groupId_P10, group.toString());
        assertEquals("yhteentoimivuus@dvv.fi", MapperUtils.propertyToString(resource, SuomiMeta.contact));
        assertTrue(MapperUtils.arrayPropertyToSet(resource, DCTerms.language).containsAll(List.of("en", "fi")));
    }

    @Test
    void mapDtoToUpdateModel() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-metadata.ttl", graphURI);
        var newOrganization = UUID.randomUUID();
        var modifiedOrig = getDate(MapperUtils.propertyToString(model.getModelResource(), DCTerms.modified));

        var dto = new TerminologyDTO();
        dto.setGraphType(GraphType.OTHER_VOCABULARY);
        dto.setLabel(Map.of("en", "Test terminology modified"));
        dto.setDescription(Map.of("en", "Test terminology description modified"));
        dto.setOrganizations(Set.of(newOrganization));
        dto.setGroups(Set.of("P10", "P11"));
        dto.setContact("new_email@dvv.fi");
        dto.setLanguages(Set.of("en"));

        var user = TestUtils.mockUser;

        TerminologyMapper.toUpdateModel(model, dto, TestUtils.categoryDTOs, user);

        var resource = model.getModelResource();

        var organization = resource.listProperties(DCTerms.contributor).toList();
        var group = resource.listProperties(DCTerms.isPartOf).toList();

        assertEquals("test", model.getPrefix());
        assertEquals(SKOS.ConceptScheme, resource.getProperty(RDF.type).getObject());
        assertEquals("Test terminology modified", MapperUtils.localizedPropertyToMap(resource, SKOS.prefLabel).get("en"));
        assertEquals("Test terminology description modified", MapperUtils.localizedPropertyToMap(resource, RDFS.comment).get("en"));
        assertEquals("new_email@dvv.fi", MapperUtils.propertyToString(resource, SuomiMeta.contact));

        assertEquals(1, organization.size());
        organization.forEach(o -> assertTrue(o.getObject().isResource()));
        var newOrganizations = organization.stream()
                .filter(o -> o.getObject().toString().equals(Constants.URN_UUID + newOrganization))
                .findFirst();
        assertTrue(newOrganizations.isPresent());

        assertEquals(2, group.size());
        group.forEach(g -> assertTrue(g.getObject().isResource()));
        var newGroups = group.stream().map(g -> g.getObject().toString()).toList();
        assertTrue(newGroups.containsAll(List.of(groupId_P10, groupId_P11)));

        var languages = MapperUtils.arrayPropertyToSet(resource, DCTerms.language);
        assertEquals(1, languages.size());
        assertTrue(languages.contains("en"));

        assertEquals(user.getId().toString(), MapperUtils.propertyToString(resource, SuomiMeta.modifier));
        var modified = getDate(MapperUtils.propertyToString(resource, DCTerms.modified));

        assertTrue(modified.isAfter(modifiedOrig));
    }

    @Test
    void mapModelToDto() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-metadata.ttl", graphURI);

        var userMapper = TestUtils.mapUser;

        var dto = TerminologyMapper.modelToDTO(model, TestUtils.categoryDTOs, TestUtils.organizationDTOs, userMapper);

        assertEquals("test", dto.getPrefix());
        assertEquals(graphURI, dto.getUri());
        assertEquals(GraphType.TERMINOLOGICAL_VOCABULARY, dto.getModelType());
        assertEquals(Status.DRAFT, dto.getStatus());
        assertEquals("Label fi", dto.getLabel().get("fi"));
        assertEquals("Label en", dto.getLabel().get("en"));
        assertEquals("Description fi", dto.getDescription().get("fi"));
        assertEquals("Description en", dto.getDescription().get("en"));
        assertTrue(dto.getLanguages().containsAll(Set.of("fi", "en")));
        assertEquals("2024-04-26T11:45:00.000Z", dto.getCreated());
        assertEquals("2024-04-26T11:46:00.000Z", dto.getModified());
        assertEquals("modifier fake-user", dto.getModifier().getName());
        assertEquals("creator fake-user", dto.getCreator().getName());
        assertEquals("yhteentoimivuus@dvv.fi", dto.getContact());

        var group = dto.getGroups().iterator().next();
        var organization = dto.getOrganizations().iterator().next();

        assertEquals("Sample category P10", group.getLabel().get("en"));
        assertEquals("P10", group.getIdentifier());
        assertEquals("Test organization", organization.getLabel().get("en"));
        assertEquals(TestUtils.organizationId.toString(), organization.getId());
    }

    @Test
    void mapIndexDto() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-metadata.ttl", graphURI);

        var indexDTO = TerminologyMapper.toIndexDocument(model, TestUtils.categoryDTOs);

        assertEquals("Label fi", indexDTO.getLabel().get("fi"));
        assertEquals("Description fi", indexDTO.getDescription().get("fi"));
        assertEquals(graphURI, indexDTO.getId());
        assertEquals("test", indexDTO.getPrefix());
        assertEquals(graphURI, indexDTO.getUri());
        assertEquals(Status.DRAFT, indexDTO.getStatus());
        assertTrue(indexDTO.getGroups().contains("P10"));
        assertTrue(indexDTO.getLanguages().containsAll(Set.of("fi", "en")));
        assertTrue(indexDTO.getOrganizations().contains(TestUtils.organizationId));
    }

    private static LocalDateTime getDate(String date) {
        // strip milliseconds from date string
        return LocalDateTime.parse(date.split("\\.")[0]);
    }
}
