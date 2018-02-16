package uk.ac.ebi.subs.api;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.Errors;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.api.services.SheetService;
import uk.ac.ebi.subs.api.validators.SheetValidator;
import uk.ac.ebi.subs.api.validators.SubsApiErrors;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.templates.Capture;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.JsonFieldType;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.SheetRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class SheetValidatorTest {

    private SheetValidator validator;

    @MockBean
    private Errors errors;

    @MockBean
    private SheetRepository sheetRepository;

    @MockBean
    private SheetService sheetService;

    @MockBean
    private OperationControlService operationControlService;


    private Submission fakeSubmission;

    @Before
    public void buildUp() {
        validator = new SheetValidator(sheetRepository, sheetService, operationControlService);

        fakeSubmission = new Submission();
        fakeSubmission.setId("fs");
    }

    @Test
    public void testNewGood() {
        Sheet sheet = exampleSheet();

        when(sheetRepository.findOne("not-a-real-id")).thenReturn(null);
        when(operationControlService.isUpdateable(fakeSubmission)).thenReturn(true);
        when(errors.hasErrors()).thenReturn(false);

        validator.validate(sheet, errors);

        assert (sheet.getRows().get(1).getDocument() != null);
        assert (sheet.getRows().get(2).getDocument() != null);
    }

    @Test
    public void testNewSubmissionLocked() {
        Sheet sheet = exampleSheet();

        when(sheetRepository.findOne("not-a-real-id")).thenReturn(null);
        when(operationControlService.isUpdateable(fakeSubmission)).thenReturn(false);
        when(errors.hasErrors()).thenReturn(true);

        validator.validate(sheet, errors);

        verify(errors).rejectValue(
                "submission",
                SubsApiErrors.resource_locked.name(),
                SubsApiErrors.resource_locked.name()
        );
    }

    @Test
    public void testNewNoTemplate() {
        Sheet sheet = exampleSheet();
        sheet.setTemplate(null);

        given(sheetRepository.findOne("not-a-real-id")).willReturn(null);
        given(operationControlService.isUpdateable(fakeSubmission)).willReturn(true);
        given(errors.hasErrors()).willReturn(true);

        validator.validate(sheet, errors);

        verify(errors).rejectValue(
                "template",
                SubsApiErrors.missing_field.name(),
                SubsApiErrors.missing_field.name()
        );
    }

    @Test
    public void testNewNoSubmission() {
        Sheet sheet = exampleSheet();
        sheet.setSubmission(null);

        given(sheetRepository.findOne("not-a-real-id")).willReturn(null);
        given(operationControlService.isUpdateable(fakeSubmission)).willReturn(true);
        given(errors.hasErrors()).willReturn(true);

        validator.validate(sheet, errors);

        verify(errors).rejectValue(
                "submission",
                SubsApiErrors.missing_field.name(),
                SubsApiErrors.missing_field.name()
        );
    }

    @Test
    public void testNewNoAlias() {
        Sheet sheet = exampleSheet();

        sheet.addRow(new String[]{"", "bob", "bob",""});

        sheet.setMappings(Arrays.asList(
                FieldCapture.builder().fieldName("alias").fieldType(JsonFieldType.String).build(),
                FieldCapture.builder().fieldName("c2").fieldType(JsonFieldType.String).build(),
                FieldCapture.builder().fieldName("c3").fieldType(JsonFieldType.String).build(),
                FieldCapture.builder().fieldName("c4").fieldType(JsonFieldType.String).build()
        ));

        when(sheetRepository.findOne("not-a-real-id")).thenReturn(null);
        given(operationControlService.isUpdateable(fakeSubmission)).willReturn(true);

        validator.validate(sheet, errors);

        verify(errors).rejectValue(
                "rows[3]",
                SubsApiErrors.missing_alias.name(),
                SubsApiErrors.missing_alias.name()
        );
    }

    @Test
    public void testBadNumberValue() {
        Sheet sheet = exampleSheet();
        sheet.getTemplate().getColumnCaptures().put("header3", FieldCapture.builder().fieldName("number").fieldType(JsonFieldType.IntegerNumber).build());

        when(sheetRepository.findOne("not-a-real-id")).thenReturn(null);
        when(operationControlService.isUpdateable(fakeSubmission)).thenReturn(true);
        when(errors.hasErrors()).thenReturn(false);

        validator.validate(sheet, errors);

        verify(errors).rejectValue(
                "rows[1].cells[2]",
                SubsApiErrors.invalid.name(),
                SubsApiErrors.invalid.name()
        );
    }


    private Stream<JSONObject> jsonObjectStreamWithAlias() {
        return jsonEmptyObjectStream().map(
                json -> {
                    json.put("alias", "bob");
                    return json;
                }
        );
    }


    private Stream<JSONObject> jsonEmptyObjectStream() {
        JSONObject json = new JSONObject();
        return Stream.of(json);
    }


    private Sheet exampleSheet() {
        Sheet sheet = new Sheet();


        sheet.setTemplate(Template.builder()
                .targetType("thing")
                .name("bob")
                .build());

        sheet.setMappings(Arrays.asList(
                FieldCapture.builder().fieldName("alias").fieldType(JsonFieldType.String).build(),
                FieldCapture.builder().fieldName("header2").fieldType(JsonFieldType.String).build(),
                FieldCapture.builder().fieldName("number").fieldType(JsonFieldType.IntegerNumber).build(),
                FieldCapture.builder().fieldName("header4").fieldType(JsonFieldType.String).build()
        ));

        sheet.setHeaderRowIndex(0);

        sheet.addRow(new String[]{"alias", "header2", "header3", "header4"});
        sheet.addRow(new String[]{"a", "", "b", "c"});
        sheet.addRow(new String[]{"4", "", "5", "6"});
        sheet.setSubmission(fakeSubmission);


        return sheet;
    }

}
