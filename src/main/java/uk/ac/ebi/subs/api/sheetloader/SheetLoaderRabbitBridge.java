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

        Spreadsheet sheet = spreadsheetRepository.findOne(sheetId);

        logger.debug("sheet ready for loading {}", sheet.getId());

        sheetLoaderService.loadSheet(sheet);

        logger.debug("sheet mapped", sheet.getId());
    }


}
