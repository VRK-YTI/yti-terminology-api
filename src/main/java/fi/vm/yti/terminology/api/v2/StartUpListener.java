package fi.vm.yti.terminology.api.v2;

import fi.vm.yti.migration.MigrationConfig;
import fi.vm.yti.migration.MigrationInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.stereotype.Component;

@Component
@ImportAutoConfiguration(MigrationConfig.class)
public class StartUpListener {

    @Autowired
    StartUpListener(MigrationInitializer migrationInitializer) {
        // TODO: should start migration automatically. Imports javax.* should be changed to jakarta.* in yti-spring-migration
        migrationInitializer.onInit();
    }
}
