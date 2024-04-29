package fi.vm.yti.terminology.api.v2;

import fi.vm.yti.common.dto.OrganizationDTO;
import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.dto.ServiceCategoryDTO;
import fi.vm.yti.common.dto.UserDTO;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestUtils {

    public static ModelWrapper getModelFromFile(String filepath, String graphURI) {
        var model = ModelFactory.createDefaultModel();
        var stream = TestUtils.class.getResourceAsStream(filepath);
        assertNotNull(stream);
        RDFDataMgr.read(model, stream, RDFLanguages.TURTLE);
        return new ModelWrapper(model, graphURI);
    }

    public static final YtiUser mockUser = new YtiUser("test@localhost",
            "test",
            "tester",
            UUID.randomUUID(),
            false,
            false,
            LocalDateTime.of(2001, 1, 1, 0,0),
            LocalDateTime.of(2001, 1, 1, 0,0),
            new HashMap<>(Map.of(UUID.randomUUID(), Set.of(Role.TERMINOLOGY_EDITOR))),
            "",
            "");

    public static final UUID organizationId = UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63");

    public static final List<ServiceCategoryDTO> categoryDTOs = List.of(
            new ServiceCategoryDTO(
                    "http://urn.fi/URN:NBN:fi:au:ptvl:v1096",
                    Map.of("en", "Sample category P10"),
                    "P10"
            ),
            new ServiceCategoryDTO(
                    "http://urn.fi/URN:NBN:fi:au:ptvl:v1097",
                    Map.of("en", "Sample category P11"),
                    "P11"
            ));

    public static final List<OrganizationDTO> organizationDTOs = List.of(new OrganizationDTO(
            organizationId.toString(),
            Map.of("en", "Test organization"),
            null)
    );

    public static Consumer<ResourceCommonInfoDTO> mapUser = (var dto) -> {
        var creator = new UserDTO("123");
        var modifier = new UserDTO("123");
        creator.setName("creator fake-user");
        modifier.setName("modifier fake-user");
        dto.setCreator(creator);
        dto.setModifier(modifier);
    };
}
