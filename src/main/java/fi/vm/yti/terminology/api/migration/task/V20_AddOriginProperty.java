package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.AttributeIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import org.springframework.stereotype.Component;

@Component
public class V20_AddOriginProperty implements MigrationTask {

    private final MigrationService migrationService;

    V20_AddOriginProperty(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {

        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {
            NodeType type = meta.getDomain().getId();

            if (type.equals(NodeType.TerminologicalVocabulary)) {
                meta.addAttribute(AttributeIndex.origin(meta.getDomain(), 21));
            }
        });
    }
}
