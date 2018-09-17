package uk.ac.ebi.subs.api.sheetloader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.RabbitMQDependentTest;
import uk.ac.ebi.subs.api.ApiIntegrationTestHelper;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.model.templates.AttributeCapture;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.JsonFieldType;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.SpreadsheetRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest(classes = ApiApplication.class)
@RunWith(SpringRunner.class)
@WithMockUser(username = "usi_admin_user", roles = {Helpers.ADMIN_TEAM_NAME})
@Category(RabbitMQDependentTest.class)
public class SheetLoaderPerfTest {

    @Autowired
    private SheetLoaderService sheetLoaderService;
    @Autowired
    private SpreadsheetRepository spreadsheetRepository;
    @Autowired
    private SampleRepository sampleRepository;
    @Autowired
    private ChecklistRepository checklistRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;
    @Autowired
    private DataTypeRepository dataTypeRepository;

    private StopWatch stopWatch = new StopWatch();

    private Submission submission;
    private Template template;
    private Spreadsheet sheet;
    private Checklist checklist;
    private DataType dataType;

    private static final int SPREADSHEET_SIZE_IN_ROWS = 3000;

    @Before
    public void init() {
        clearDbs();

        ApiIntegrationTestHelper.initialiseDataTypes(dataTypeRepository);

        submission = Helpers.generateSubmission();

        submissionRepository.insert(submission);

        SubmissionStatus submissionStatus = new SubmissionStatus();
        submissionStatus.setStatus(SubmissionStatusEnum.Draft);
        submissionStatus.setTeam(submission.getTeam());
        submissionStatusRepository.save(submissionStatus);

        submission.setSubmissionStatus(submissionStatus);
        submissionRepository.save(submission);


        dataType = new DataType();
        dataType.setId("dt");
        dataType.setSubmittableClassName(Sample.class.getName());
        dataTypeRepository.insert(dataType);

        checklist = new Checklist();
        checklist.setId("foo");
        checklist.setDataTypeId(dataType.getId());


        template = template();
        checklist.setSpreadsheetTemplate(template);
        checklistRepository.insert(checklist);

        sheet = sheet();
        spreadsheetRepository.insert(sheet);
    }

    @Test
    public void timedTest() {
        stopWatch.start();

        sheetLoaderService.loadSheet(sheet);

        stopWatch.stop();
        System.out.println(stopWatch.shortSummary());
    }


    private Spreadsheet sheet() {
        Spreadsheet sheet = new Spreadsheet();
        sheet.setChecklistId(checklist.getId());
        sheet.setDataTypeId(dataType.getId());
        sheet.setTeam(submission.getTeam());
        sheet.setSubmissionId(submission.getId());
        List<String> headers = template.getColumnCaptures().keySet().stream().collect(Collectors.toList());
        sheet.setHeaderRow(new Row(headers));
        sheet.setRows(createRows());
        return sheet;
    }

    private List<Row> createRows() {
        List<Row> rows = new LinkedList<>();
        List<JsonFieldType> types = Arrays.asList(
                JsonFieldType.String, JsonFieldType.String, JsonFieldType.String, JsonFieldType.String,
                JsonFieldType.IntegerNumber
        );

        for (int i = 0; i < SPREADSHEET_SIZE_IN_ROWS; i++) {
            List<String> cells = new LinkedList<>();


            for (JsonFieldType type : types) {
                String value;

                if (type == JsonFieldType.IntegerNumber) {
                    int num = ThreadLocalRandom.current().nextInt(1000, 10001);
                    value = "" + num;
                } else {
                    value = UUID.randomUUID().toString();
                }

                cells.add(value);
            }
            rows.add(new Row(cells));

        }

        return rows;
    }

    @After
    public void clearDbs() {
        Stream.of(
                spreadsheetRepository,
                sampleRepository,
                checklistRepository,
                submissionRepository,
                submissionStatusRepository,
                dataTypeRepository)
                .forEach(CrudRepository::deleteAll);
    }

    private Template template() {
        Template template = new Template();

        template
                .add(
                        "unique name",
                        FieldCapture.builder().fieldName("alias").build()
                )
                .add("title",
                        FieldCapture.builder().fieldName("title").build()
                )
                .add(
                        "description",
                        FieldCapture.builder().fieldName("description").build()
                )
                .add("taxon",
                        FieldCapture.builder().fieldName("taxon").build()
                )
                .add("taxon id",
                        FieldCapture.builder().fieldName("taxonId").fieldType(JsonFieldType.IntegerNumber).build()
                );

        template.setDefaultCapture(
                AttributeCapture.builder().build()
        );


        return template;
    }
}
