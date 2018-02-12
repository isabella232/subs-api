package uk.ac.ebi.subs.api;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.api.handlers.SheetHandler;
import uk.ac.ebi.subs.api.services.SheetService;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.Template;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.notNull;



@RunWith(SpringRunner.class)
public class SheetEventHandlerTest {

    private SheetHandler sheetHandler;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @MockBean
    private SheetService sheetService;

    @Before
    public void buildUp(){
        sheetHandler = new SheetHandler(rabbitMessagingTemplate,sheetService);
    }

    @Test
    public void testBeforeCreate(){
        Sheet sheet = exampleSheet();

        sheetHandler.handleBeforeCreate(sheet);

        verify(sheetService).preProcessSheet(sheet);

        assertThat(sheet.getId(), notNullValue());
        assertThat(sheet.getStatus(),is(equalTo(SheetStatusEnum.Submitted)));
    }

    @Test
    public void testAfterCreate(){
        Sheet sheet = exampleSheet();
        sheet.setStatus(SheetStatusEnum.Submitted);

        sheetHandler.handleAfterCreate(sheet);

        verify(rabbitMessagingTemplate).convertAndSend(Exchanges.SUBMISSIONS,"usi.sheet.submitted",sheet);

    }


    private Sheet exampleSheet() {
        Sheet sheet = new Sheet();
        sheet.setTemplate(Template.builder().targetType("thing").name("bob").build());
        sheet.setHeaderRowIndex(1);
        sheet.addRow(new String[]{"header1", "header2", "header3", "header4"});
        sheet.addRow(new String[]{"a", "", "b", "c"});
        sheet.addRow(new String[]{"4", "", "5", "6"});


        sheet.setMappings(Arrays.asList(
                FieldCapture.builder().fieldName("alias").build()
        ));

        return sheet;
    }

}
