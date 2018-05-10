package uk.ac.ebi.subs.api.handlers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.api.model.FileDeleteMessage;
import uk.ac.ebi.subs.api.utils.FileHelper;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class FileDeletionEventHandlerTest {

    private static final String SUBMISSION_ID = "1234ABCD";

    private FileDeletionEventHandler fileDeletionEventHandler;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @MockBean
    private ValidationResultRepository validationResultRepository;

    @Before
    public void buildUp() {
        fileDeletionEventHandler = new FileDeletionEventHandler(rabbitMessagingTemplate, validationResultRepository);
    }

    @Test
    public void testAfterCreate() {
        File file = FileHelper.createFile(SUBMISSION_ID);

        fileDeletionEventHandler.handleAfterFileDeletion(file);

        verify(rabbitMessagingTemplate).convertAndSend(eq(Exchanges.SUBMISSIONS), eq("usi.file.deletion"), any(FileDeleteMessage.class));
    }


}
