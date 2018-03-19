package uk.ac.ebi.subs.api.sheetloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.repos.SheetRepository;

@Component
public class SheetLoaderRabbitBridge {

    private static final Logger logger = LoggerFactory.getLogger(SheetLoaderRabbitBridge.class);

    private RabbitMessagingTemplate rabbitMessagingTemplate;
    private SheetLoaderService sheetLoaderService;
    private SheetRepository sheetRepository;
    private AdminUserService adminUserService;

    public SheetLoaderRabbitBridge(RabbitMessagingTemplate rabbitMessagingTemplate, SheetLoaderService sheetLoaderService, SheetRepository sheetRepository, AdminUserService adminUserService) {
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
        this.sheetLoaderService = sheetLoaderService;
        this.sheetRepository = sheetRepository;
        this.adminUserService = adminUserService;
    }

    @RabbitListener(queues = SheetLoaderQueueConfig.SHEET_SUBMITTED_QUEUE)
    public void onSubmissionLoadSheetContents(String sheetId) {

        adminUserService.injectAdminUserIntoSecurityContext();

        logger.debug("sheet ready for loading {}", sheetId);

        Sheet sheet = sheetRepository.findOne(sheetId);
        sheetLoaderService.loadSheet(sheet);
        logger.debug("sheet mapped", sheetId);

    }


}
