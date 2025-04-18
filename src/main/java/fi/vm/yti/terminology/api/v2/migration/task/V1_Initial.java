package fi.vm.yti.terminology.api.v2.migration.task;

import fi.vm.yti.migration.MigrationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S101")
@Component
public class V1_Initial implements MigrationTask {

    private static final Logger LOG = LoggerFactory.getLogger(V1_Initial.class.getName());

    @Override
    public void migrate() {
        LOG.info("Initial migration");
    }
}
