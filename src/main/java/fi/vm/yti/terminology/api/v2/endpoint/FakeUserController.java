package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.common.dto.GroupManagementUserDTO;
import fi.vm.yti.common.service.CommonGroupManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FakeUserController {

    private final CommonGroupManagementService groupManagementService;

    FakeUserController(CommonGroupManagementService groupManagementService) {
        this.groupManagementService = groupManagementService;
    }

    @GetMapping
    @Operation(description = "Get fake users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of user objects")
    })
    public List<GroupManagementUserDTO> getFakeUsers() {
        return groupManagementService.getFakeableUsers();
    }
}
