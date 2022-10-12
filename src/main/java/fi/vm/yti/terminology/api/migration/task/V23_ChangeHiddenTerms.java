package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

@Component
public class V23_ChangeHiddenTerms implements MigrationTask {

    private static Logger log = LoggerFactory.getLogger(V23_ChangeHiddenTerms.class);

    private final MigrationService migrationService;

    V23_ChangeHiddenTerms(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {
        List<GenericNode> concepts = migrationService.getNodes(
                node -> node.getType().getId().equals(NodeType.Concept)
                        && node.getReferences().containsKey("hiddenTerm"));

        log.info("Updating hiddenTerms for {} concepts", concepts.size());

        for (var concept : concepts) {
            List<Identifier> searchTerms = concept.getReferences().getOrDefault("searchTerm", new ArrayList<>());
            List<Identifier> hiddenTerms = concept.getReferences().getOrDefault("hiddenTerm", new ArrayList<>());

            log.info("Found {} hiddenTerms for concept {}. Previously found {} searchTerms",
                    hiddenTerms.size(), concept.getId(), searchTerms.size());

            searchTerms.addAll(hiddenTerms);

            concept.getReferences().putIfAbsent("searchTerm", searchTerms);
            concept.getReferences().remove("hiddenTerm");
        }

        migrationService.updateAndDeleteInternalNodes(new GenericDeleteAndSave(emptyList(), concepts));
        log.info("Migration of hiddenTerms completed");
    }

}
