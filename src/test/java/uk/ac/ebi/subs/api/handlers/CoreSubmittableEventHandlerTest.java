package uk.ac.ebi.subs.api.handlers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.api.model.StoredSubmittableDeleteMessage;
import uk.ac.ebi.subs.api.services.SubmittableValidationDispatcher;
import uk.ac.ebi.subs.api.utils.SubmittableHelper;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.AssayData;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;
import uk.ac.ebi.subs.repository.services.SubmittableHelperService;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class CoreSubmittableEventHandlerTest {

    private static final String SUBMISSION_ID = "1234ABCD";

    private CoreSubmittableEventHandler coreSubmittableEventHandler;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;
    @MockBean
    private ValidationResultRepository validationResultRepository;
    @MockBean
    private SubmittableHelperService submittableHelperService;
    @MockBean
    private SubmittableValidationDispatcher submittableValidationDispatcher;
    @MockBean
    private ProcessingStatusRepository processingStatusRepository;
    @MockBean
    private Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap;

    @Before
    public void buildUp() {
        coreSubmittableEventHandler = new CoreSubmittableEventHandler(
                rabbitMessagingTemplate,
                submittableHelperService,
                submittableValidationDispatcher,
                validationResultRepository,
                processingStatusRepository,
                submittableRepositoryMap
        );
    }

    @Test
    public void testHandleAfterDelete_assayDataDeletionShouldTriggerMessageSending() {
        AssayData assayData = SubmittableHelper.createAssayData(SUBMISSION_ID);

        coreSubmittableEventHandler.handleAfterSubmittableDeletion(assayData);

        verify(rabbitMessagingTemplate).convertAndSend(
                eq(Exchanges.SUBMISSIONS), eq(CoreSubmittableEventHandler.STORED_SUBMITTABLE_DELETION_ROUTING_KEY),
                any(StoredSubmittableDeleteMessage.class));
    }
}
