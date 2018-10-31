package uk.ac.ebi.subs.api.sheetloader;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.subs.messaging.Queues;

/**
 * This configuration class responsible for the RabbitMQ configuration for the sheet loader service.
 */
@Configuration
public class SheetLoaderQueueConfig {

    final static String SHEET_SUBMITTED_QUEUE = "usi-sheetId-submitted-load-contents";
    private final String SHEET_SUBMITTED_ROUTING_KEY = "usi.sheetId.submitted";

    /**
     * Queue for cleaning up contents of a submission if the user deletes it
     */
    @Bean
    Queue onSubmitLoadSheetQueue(){return Queues.buildQueueWithDlx(SHEET_SUBMITTED_QUEUE);}

    @Bean
    Binding onSubmitLoadSheetBinding(Queue onSubmitLoadSheetQueue, TopicExchange submissionExchange) {
        return BindingBuilder.bind(onSubmitLoadSheetQueue).to(submissionExchange).with(SHEET_SUBMITTED_ROUTING_KEY);
    }
}
