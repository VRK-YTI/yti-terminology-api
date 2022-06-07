package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.AttributeIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import org.springframework.stereotype.Component;

@Component
public class V22_LinkAndSubjectAreaProperties implements MigrationTask  {
    private final MigrationService migrationService;

    V22_LinkAndSubjectAreaProperties(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {

        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {
            NodeType type = meta.getDomain().getId();

            if (type.equals(NodeType.Concept)) {
                meta.addAttribute(AttributeIndex.externalLink(meta.getDomain(), 30));
                meta.addAttribute(AttributeIndex.subjectArea(meta.getDomain(), 35));
            }

        });
    }
}
