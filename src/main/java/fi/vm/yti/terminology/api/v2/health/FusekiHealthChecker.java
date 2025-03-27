package fi.vm.yti.terminology.api.v2.health;

import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "health.fuseki.enabled", havingValue = "true", matchIfMissing = true)
public class FusekiHealthChecker implements HealthIndicator {
    private final TerminologyRepository repository;

    public FusekiHealthChecker(TerminologyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health health() {
        try {
            return repository.isHealthy() ?
                    Health.up().withDetail("fuseki_connection", "OK").build() :
                    Health.down().withDetail("fuseki_connection", "NOT OK").build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("fuseki_connection", "NOT OK")
                    .withDetail("exception", e.getMessage())
                    .build();
        }
    }
}
