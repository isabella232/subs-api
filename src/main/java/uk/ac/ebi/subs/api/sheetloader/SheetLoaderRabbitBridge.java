package uk.ac.ebi.subs.api.sheetloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.repos.SheetRepository;
import uk.ac.ebi.subs.repository.security.RoleLookup;
import uk.ac.ebi.tsc.aap.client.model.Domain;

import java.util.Arrays;

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
    public void onSubmissionLoadSheetContents(Sheet sheet) {

        adminUserService.injectAdminUserIntoSecurityContext();

        logger.info("sheet ready for loading {}", sheet.getId());

        sheetLoaderService.loadSheet(sheet);

        logger.info("sheet mapped", sheet.getId());
    }


}
