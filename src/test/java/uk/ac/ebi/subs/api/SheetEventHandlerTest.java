package uk.ac.ebi.subs.api;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.api.handlers.SheetHandler;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.verify;


@RunWith(SpringRunner.class)
public class SheetEventHandlerTest {

    private SheetHandler sheetHandler;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;


    @Before
    public void buildUp() {
        sheetHandler = new SheetHandler(rabbitMessagingTemplate);
    }

    @Test
    public void testBeforeCreate() {
        Sheet sheet = exampleSheet();

        sheetHandler.handleBeforeCreate(sheet);

        assertThat(sheet.getId(), notNullValue());
        assertThat(sheet.getStatus(), is(equalTo(SheetStatusEnum.Submitted)));
    }

    @Test
    public void testAfterCreate() {
        Sheet sheet = exampleSheet();
        sheet.setStatus(SheetStatusEnum.Submitted);

        sheetHandler.handleAfterCreate(sheet);

        verify(rabbitMessagingTemplate).convertAndSend(Exchanges.SUBMISSIONS, "usi.sheet.submitted", sheet);
    }


    private Sheet exampleSheet() {
        Sheet sheet = new Sheet();

        sheet.setSourceFileName("my-samples.csv");

        sheet.setHeaderRow(new Row(new String[]{"col1", "col2", "col3"}));
        sheet.addRow(new String[]{"a", "b", "c"});

        return sheet;
    }

}
