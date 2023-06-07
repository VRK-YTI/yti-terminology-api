package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class V25_RemoveTermedSuffix implements MigrationTask {

    private static final Logger log = LoggerFactory.getLogger(V25_RemoveTermedSuffix.class);


    private final MigrationService migrationService;

    V25_RemoveTermedSuffix(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {
        var nodes = migrationService.getNodes(node -> node.getUri() != null && node.getUri().matches("http://uri.suomi.fi/terminology/[a-zA-Z0-9-_]*/terminological-vocabulary-[0-9]+"));
        log.info("Updating hiddenTerms for {} concepts", nodes.size());

        nodes.forEach(node -> {
            var newUri = node.getUri().replaceAll("/terminological-vocabulary-[0-9]*", "");
            node.setUri(newUri);
        });

        migrationService.updateAndDeleteInternalNodes(new GenericDeleteAndSave(Collections.emptyList(), nodes));
    }
}