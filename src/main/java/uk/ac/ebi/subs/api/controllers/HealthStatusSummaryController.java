package uk.ac.ebi.subs.api.controllers;

import org.springframework.boot.actuate.amqp.RabbitHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.mongo.MongoHealthIndicator;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simplified health reporting for the load balancer - return UP or DOWN only.
 *
 */
@RestController
public class HealthStatusSummaryController {

    private MongoHealthIndicator mongoHealthIndicator;
    private RabbitHealthIndicator rabbitHealthIndicator;
    private DiskSpaceHealthIndicator diskSpaceHealthIndicator;

    @RequestMapping(value = "/health/summary", produces = {MediaType.TEXT_PLAIN_VALUE})
    public String healthStatusSummary() {

        Status status = Status.DOWN;

        if (mongoHealthIndicator.getHealth(false).getStatus() == Status.UP
                && rabbitHealthIndicator.getHealth(false).getStatus() == Status.UP
                && diskSpaceHealthIndicator.getHealth(false).getStatus() == Status.UP
        ) {
            status = Status.UP;
        }

        return status.toString();
    }
}
