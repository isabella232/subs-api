package uk.ac.ebi.subs.api.sheetloader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.RabbitMQDependentTest;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.templates.AttributeCapture;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.JsonFieldType;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.SheetRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.TemplateRepository;
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
public class SheetLoaderPerfTest implements RabbitMQDependentTest {

    @Autowired
    private SheetLoaderService sheetLoaderService;
    @Autowired
    private SheetRepository sheetRepository;
    @Autowired
    private SampleRepository sampleRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;

    private StopWatch stopWatch = new StopWatch();

    private Submission submission;
    private Template template;
    private Sheet sheet;

    private static final int SPREADSHEET_SIZE_IN_ROWS = 3000;

    @Before
    public void init() {
        clearDbs();

        submission = Helpers.generateSubmission();

        submissionRepository.insert(submission);

        SubmissionStatus submissionStatus = new SubmissionStatus();
        submissionStatus.setStatus(SubmissionStatusEnum.Draft);
        submissionStatus.setTeam(submission.getTeam());
        submissionStatusRepository.save(submissionStatus);

        submission.setSubmissionStatus(submissionStatus);
        submissionRepository.save(submission);


        template = template();
        templateRepository.insert(template);

        sheet = sheet();
        sheetRepository.insert(sheet);
    }

    @Test
    public void timedTest() {
        stopWatch.start();

        sheetLoaderService.loadSheet(sheet);

        stopWatch.stop();
        System.out.println(stopWatch.shortSummary());
    }


    private Sheet sheet() {
        Sheet sheet = new Sheet();
        sheet.setTemplate(template);
        sheet.setTeam(submission.getTeam());
        sheet.setSubmission(submission);
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
        Stream.of(sheetRepository, sampleRepository, templateRepository, submissionRepository, submissionStatusRepository)
                .forEach(CrudRepository::deleteAll);
    }

    private Template template() {
        Template template = Template.builder()
                .name("samples-template")
                .targetType("samples")
                .build();

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

        template.setId(UUID.randomUUID().toString());

        return template;
    }
}
