package uk.ac.ebi.subs.api.handlers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.services.SheetService;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.SubmittablesBatch;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@RepositoryEventHandler(SubmittablesBatch.class)
public class SubmittablesBatchHandler {

    @NonNull
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    private final String routingKeyPrefix = "usi.submittablesBatch.";


    @HandleBeforeCreate
    public void handleBeforeCreate(SubmittablesBatch batch) {
        this.fillInId(batch);
        //we only support immediate submission at the moment
        batch.setStatus("Submitted");
    }

    private void fillInId(SubmittablesBatch batch) {
        batch.setId(UUID.randomUUID().toString());
    }

    @HandleAfterCreate
    public void handleAfterCreate(SubmittablesBatch batch) {
        sendSheetEvent(batch);
    }

    @HandleAfterSave
    public void handleAfterSave(SubmittablesBatch batch) {
        sendSheetEvent(batch);
    }

    private void sendSheetEvent(SubmittablesBatch batch) {
        String routingKey = routingKeyPrefix + batch.getStatus().toLowerCase();

        rabbitMessagingTemplate.convertAndSend(
                Exchanges.SUBMISSIONS,
                routingKey,
                batch);
    }


}
