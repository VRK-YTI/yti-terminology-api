package fi.vm.yti.terminology.api.v2;

import fi.vm.yti.migration.MigrationConfig;
import fi.vm.yti.migration.MigrationInitializer;
import fi.vm.yti.terminology.api.v2.opensearch.OpenSearchIndexer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.stereotype.Component;

@Component
@ImportAutoConfiguration(MigrationConfig.class)
public class StartUpListener {

    private final OpenSearchIndexer openSearchIndexer;

    @Autowired
    StartUpListener(MigrationInitializer migrationInitializer,
                    OpenSearchIndexer openSearchIndexer) {
        this.openSearchIndexer = openSearchIndexer;
        // TODO: should start migration automatically. Imports javax.* should be changed to jakarta.* in yti-spring-migration
        migrationInitializer.onInit();
    }

    @PostConstruct
    public void contextInitialized() {
        openSearchIndexer.initIndexes();
    }
}
