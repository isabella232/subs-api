package uk.ac.ebi.subs.api.handlers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.services.SheetService;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@RepositoryEventHandler(Sheet.class)
public class SheetHandler {

    @NonNull
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @NonNull
    private SheetService sheetService;

    private final String routingKeyPrefix = "usi.sheet.";


    @HandleBeforeCreate
    public void handleBeforeCreate(Sheet sheet) {
        sheetService.preProcessSheet(sheet);

        this.fillInId(sheet);
        //we only support immediate submission at the moment
        sheet.setStatus(SheetStatusEnum.Submitted);
    }

    private void fillInId(Sheet sheet) {
        sheet.setId(UUID.randomUUID().toString());
    }

    @HandleAfterSave
    public void handleAfterCreate(Sheet sheet) {
        sendSheetEvent(sheet);
    }

    @HandleAfterSave
    public void handleAfterSave(Sheet sheet) {
        sendSheetEvent(sheet);
    }

    private void sendSheetEvent(Sheet sheet) {
        String routingKey = routingKeyPrefix + sheet.getStatus().name().toLowerCase();

        rabbitMessagingTemplate.convertAndSend(
                Exchanges.SUBMISSIONS,
                routingKey,
                sheet);
    }


}
