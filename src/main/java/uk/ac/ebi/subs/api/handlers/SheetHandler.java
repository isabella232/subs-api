package uk.ac.ebi.subs.api.handlers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;

import java.util.UUID;

/**
 * This class is responsible handling events related to {@link Spreadsheet} entity.
 */
@Component
@RequiredArgsConstructor
@RepositoryEventHandler(Spreadsheet.class)
public class SheetHandler {

    @NonNull
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    private final String routingKeyPrefix = "usi.sheetId.";


    @HandleBeforeCreate
    public void handleBeforeCreate(Spreadsheet sheet) {
        this.fillInId(sheet);
        //we only support immediate submission at the moment
        sheet.setStatus(SheetStatusEnum.Submitted);
    }

    private void fillInId(Spreadsheet sheet) {
        sheet.setId(UUID.randomUUID().toString());
    }


    @HandleAfterCreate
    public void handleAfterCreate(Spreadsheet sheet) {
        sendSheetEvent(sheet);
    }

    @HandleAfterSave
    public void handleAfterSave(Spreadsheet sheet) {
        sendSheetEvent(sheet);
    }

    private void sendSheetEvent(Spreadsheet sheet) {
        String routingKey = routingKeyPrefix + sheet.getStatus().name().toLowerCase();

        rabbitMessagingTemplate.convertAndSend(
                Exchanges.SUBMISSIONS,
                routingKey,
                sheet.getId());
    }


}
