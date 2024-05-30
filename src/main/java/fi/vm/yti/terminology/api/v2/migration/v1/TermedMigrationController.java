package fi.vm.yti.terminology.api.v2.migration.v1;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;

@RestController
@RequestMapping("v2/migrate")
@Tag(name = "Migrate")
@Hidden
public class TermedMigrationController {

    private final TermedMigrationService migrationService;

    public TermedMigrationController(TermedMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping
    void migrate(@RequestParam String terminologyId) throws URISyntaxException {
        migrationService.migrate(terminologyId);
    }

    @PostMapping("/all")
    void migrateAll() throws URISyntaxException {
        migrationService.migrateAll();
    }
}
