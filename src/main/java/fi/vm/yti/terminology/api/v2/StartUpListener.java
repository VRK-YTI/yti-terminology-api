package fi.vm.yti.terminology.api.v2;

import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.migration.MigrationConfig;
import fi.vm.yti.migration.MigrationInitializer;
import fi.vm.yti.terminology.api.v2.service.IndexService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.stereotype.Component;

@Component
@ImportAutoConfiguration(MigrationConfig.class)
public class StartUpListener {

    private final IndexService indexService;

    private final GroupManagementService groupManagementService;

    @Autowired
    StartUpListener(MigrationInitializer migrationInitializer,
                    IndexService indexService,
                    GroupManagementService groupManagementService) {
        this.indexService = indexService;
        this.groupManagementService = groupManagementService;
        // TODO: should start migration automatically. Imports javax.* should be changed to jakarta.* in yti-spring-migration
        migrationInitializer.onInit();
    }

    @PostConstruct
    public void contextInitialized() {
        indexService.initIndexes();
        groupManagementService.initOrganizations();
        groupManagementService.initUsers();
    }
}
