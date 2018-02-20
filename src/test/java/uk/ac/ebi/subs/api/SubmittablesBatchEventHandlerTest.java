package uk.ac.ebi.subs.api;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.api.handlers.SubmittablesBatchHandler;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.SubmittablesBatch;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.Template;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.notNull;



@RunWith(SpringRunner.class)
public class SubmittablesBatchEventHandlerTest {

    private SubmittablesBatchHandler submittablesBatchHandler;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;


    @Before
    public void buildUp(){
        submittablesBatchHandler = new SubmittablesBatchHandler(rabbitMessagingTemplate);
    }

    @Test
    public void testBeforeCreate(){
        SubmittablesBatch batch = exampleBatch();

        submittablesBatchHandler.handleBeforeCreate(batch);

        assertThat(batch.getId(), notNullValue());
        assertThat(batch.getStatus(),is(equalTo("Submitted")));
    }

    @Test
    public void testAfterCreate(){
        SubmittablesBatch batch = exampleBatch();
        batch.setStatus("Submitted");

        submittablesBatchHandler.handleAfterCreate(batch);

        verify(rabbitMessagingTemplate).convertAndSend(Exchanges.SUBMISSIONS,"usi.submittablesBatch.submitted",batch);

    }


    private SubmittablesBatch exampleBatch() {
        SubmittablesBatch batch = new SubmittablesBatch();

        batch.setName("my-samples.csv");
        batch.setTargetType("samples");
        batch.addDocument(new SubmittablesBatch.Document());

        return batch;
    }

}
