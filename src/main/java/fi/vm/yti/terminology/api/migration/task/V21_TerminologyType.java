package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.AttributeIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import org.springframework.stereotype.Component;

/**
 * TerminologyType acts as a "label" to indicate whether the vocabulary is maintained by professional terminology. The purpose of
 * this property is different from using TypeId to separate different types of terminology nodes. In this case each terminology
 * type contains the same information. Separating with TypeId, each type can contain different set of metanodes and properties.
 *
 * @see fi.vm.yti.terminology.api.frontend.TerminologyType
 */
@Component
public class V21_TerminologyType implements MigrationTask {

    private final MigrationService migrationService;

    V21_TerminologyType(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {

        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {
            NodeType type = meta.getDomain().getId();

            if (type.equals(NodeType.TerminologicalVocabulary)) {
                meta.addAttribute(AttributeIndex.terminologyType(meta.getDomain(), 25));
            }
        });
    }
}
