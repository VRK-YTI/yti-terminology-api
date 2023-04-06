package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.DomainIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.migration.ReferenceIndex;
import fi.vm.yti.terminology.api.model.termed.GraphId;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.TypeId;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import org.springframework.stereotype.Component;

@Component
public class V24_NewConceptRelations implements MigrationTask {

    private final MigrationService migrationService;
    V24_NewConceptRelations(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {
        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {
            var domain = meta.getDomain();
            if (domain.getId().equals(NodeType.Concept)) {
                var externalLinkDomain = new TypeId(NodeType.ConceptLink, new GraphId(domain.getGraphId()));
                meta.addReference(ReferenceIndex.broadMatch(domain, externalLinkDomain, 35));
                meta.addReference(ReferenceIndex.narrowMatch(domain, externalLinkDomain, 40));
            }
        });
    }
}
