package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.Attribute;
import fi.vm.yti.terminology.api.model.termed.GenericDeleteAndSave;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import static java.util.Collections.emptyList;

/**
 * Adds status property to concepts and terms if they are missing
 */
@Component
public class V26_UpdateMissingStatuses implements MigrationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(V26_UpdateMissingStatuses.class);

    private final MigrationService migrationService;

    private static final String STATUS = "status";

    V26_UpdateMissingStatuses(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {
        var types = List.of(NodeType.Concept, NodeType.Term);
        var nodesWithoutStatus = migrationService.getNodes(node -> types.contains(node.getType().getId())
                && node.getProperties().get(STATUS) == null);

        LOGGER.info("Updating status for {} nodes", nodesWithoutStatus.size());

        nodesWithoutStatus.forEach(node -> node.getProperties()
                .put(STATUS, List.of(new Attribute("", Status.DRAFT.name()))));

        migrationService.updateAndDeleteInternalNodes(new GenericDeleteAndSave(emptyList(), nodesWithoutStatus));
    }
}
