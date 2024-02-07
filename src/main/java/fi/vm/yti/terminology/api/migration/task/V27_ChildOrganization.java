package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.DomainIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.ReferenceMeta;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static fi.vm.yti.terminology.api.migration.PropertyUtil.*;
import static java.util.Collections.emptyMap;

@Component
public class V27_ChildOrganization implements MigrationTask {

    private final MigrationService migrationService;

    V27_ChildOrganization(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {
        UUID graphId = DomainIndex.ORGANIZATION_DOMAIN.getGraph().getId();
        migrationService.updateTypes(graphId, metaNode -> {
            if (metaNode.getDomain().getId().equals(NodeType.Organization)) {

                ReferenceMeta attribute = new ReferenceMeta(
                        metaNode.getDomain(),
                        "parent",
                        "http://uri.suomi.fi/datamodel/ns/iow#parentOrganization",
                        2L,
                        metaNode.getDomain(),
                        emptyMap(),
                        prefLabel(
                                "Yläorganisaatio",
                                "Parent organization"
                        )

                );
                metaNode.addReference(attribute);
            }
        });
    }

}
