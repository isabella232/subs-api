package uk.ac.ebi.subs.api.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simplified health reporting - return UP or DOWN only
 *
 */
@RestController
public class HealthStatusSummaryController {

    private HealthIndicator healthIndicator;

    public HealthStatusSummaryController(HealthAggregator healthAggregator,
                          Map<String, HealthIndicator> healthIndicators) {

        Assert.notNull(healthAggregator, "HealthAggregator must not be null");
        Assert.notNull(healthIndicators, "HealthIndicators must not be null");
        CompositeHealthIndicator healthIndicator = new CompositeHealthIndicator(
                healthAggregator);

        for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
            healthIndicator.addHealthIndicator(entry.getKey(), entry.getValue());
        }
        this.healthIndicator = healthIndicator;
    }


    @RequestMapping(value = "/health/summary", produces = {MediaType.TEXT_PLAIN_VALUE})
    public String healthStatusSummary() {
        return healthIndicator.health().getStatus().toString();
    }


}
