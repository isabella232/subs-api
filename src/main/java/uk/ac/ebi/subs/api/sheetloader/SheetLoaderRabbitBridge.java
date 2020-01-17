package uk.ac.ebi.subs.api.sheetloader;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.repos.SpreadsheetRepository;

/**
 * This is a listener class triggered by a {@link Spreadsheet} message is published in the SHEET_SUBMITTED_QUEUE queue.
 */
@Component
@RequiredArgsConstructor
public class SheetLoaderRabbitBridge {

    private static final Logger logger = LoggerFactory.getLogger(SheetLoaderRabbitBridge.class);

    @NonNull
    private SheetLoaderService sheetLoaderService;
    @NonNull
    private SpreadsheetRepository spreadsheetRepository;
    @NonNull
    private AdminUserService adminUserService;


    @RabbitListener(queues = SheetLoaderQueueConfig.SHEET_SUBMITTED_QUEUE)
    public void onSubmissionLoadSheetContents(String sheetId) {

        adminUserService.injectAdminUserIntoSecurityContext();

        Spreadsheet sheet = spreadsheetRepository.findById(sheetId).orElse(null);

        logger.debug("sheet ready for loading {}", sheet.getId());

        sheetLoaderService.loadSheet(sheet);

        logger.debug("sheet mapped", sheet.getId());
    }


}
