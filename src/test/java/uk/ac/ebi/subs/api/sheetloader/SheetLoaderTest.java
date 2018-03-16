package uk.ac.ebi.subs.api.sheetloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.hateoas.RelProvider;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.model.templates.AttributeCapture;
import uk.ac.ebi.subs.repository.model.templates.Capture;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.JsonFieldType;
import uk.ac.ebi.subs.repository.model.templates.NoOpCapture;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.SheetRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;
import uk.ac.ebi.subs.repository.services.SubmittableHelperService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class SheetLoaderTest {


    private SheetLoaderService sheetLoaderService;

    @MockBean
    private SheetRepository sheetRepository;

    @MockBean
    private SampleRepository sampleRepository;

    @MockBean
    private ApplicationEventPublisher publisher;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private SubmittableHelperService submittableHelperService;

    @MockBean
    private RelProvider relProvider;

    private Submission submission;

    @Before
    public void setUp() {
        Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>>
                submittableRepositoryMap = new HashMap<>();
        submittableRepositoryMap.put(Sample.class, sampleRepository);

        sheetLoaderService = new SheetLoaderService(
                submittableRepositoryMap,
                publisher,
                sheetRepository,
                relProvider,
                objectMapper,
                submittableHelperService
        );


        submission = new Submission();
        submission.setTeam(Team.build("test"));
        submission.setId("1234");
        this.sheet = sheet(submission);

        Template template = template();
        Map<String, Capture> captureMap = template.getColumnCaptures();

        expectedCaptures = Arrays.asList(
                captureMap.get("unique name"),
                captureMap.get("title"),
                captureMap.get("description"),
                captureMap.get("taxon"),
                captureMap.get("taxon id"),
                AttributeCapture.builder().build(),
                NoOpCapture.builder().build()
        );

        when(relProvider.getCollectionResourceRelFor(Sample.class)).thenReturn("samples");

    }

    private Sheet sheet;
    private List<Capture> expectedCaptures;


    @Test
    public void testMappingHeadersToColumnCaptures() {

        List<Capture> actualColumnMappings = sheetLoaderService.mapColumns(
                sheet.getHeaderRow(),
                sheet.getTemplate().getColumnCaptures(),
                Optional.of(sheet.getTemplate().getDefaultCapture())
        );

        assertThat(actualColumnMappings, equalTo(expectedCaptures));
    }

    @Test
    public void convert_row_to_document() {
        JSONObject expectedJson = stringToJsonObject(
                "{\n" +
                        "  \"alias\": \"s1\",\n" +
                        "  \"taxon\": \"Homo sapiens\",\n" +
                        "  \"taxonId\": 9606,\n" +
                        "  \"description\": \"\",\n" +
                        "  \"title\": \"\",\n" +
                        "  \"attributes\": {\n" +
                        "    \"height\": [\n" +
                        "      {\n" +
                        "        \"value\": \"1.7\",\n" +
                        "        \"units\": \"meters\"\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}");


        JSONObject actualJson = sheetLoaderService.rowToDocument(
                sheet.getRows().get(0),
                expectedCaptures,
                sheet.getHeaderRow().getCells()
        );

        JSONAssert.assertEquals(expectedJson, actualJson, true);
        assertTrue(sheet.getRows().get(0).isProcessed());
    }


    @Test
    public void load_one_new_sample() {
        Sample s = new Sample();
        s.setAlias("test1");
        s.setTaxonId(7L);

        Row row = new Row();

        when(sampleRepository.findOneBySubmissionIdAndAlias(submission.getId(), s.getAlias()))
                .thenReturn(null);

        sheetLoaderService.storeSubmittable(submission, row, s);

        verify(sampleRepository).findOneBySubmissionIdAndAlias(submission.getId(), s.getAlias());
        verify(publisher).publishEvent(any(BeforeCreateEvent.class));
        verify(sampleRepository).insert(same(s));
        verify(submittableHelperService).processingStatusAndValidationResultSetUp(same(s));

    }

    @Test
    public void load_one_existing_sample() {
        Sample s = new Sample();
        s.setAlias("test1");
        s.setTaxonId(7L);

        Sample storedVersion = new Sample();
        storedVersion.setAlias("test1");
        storedVersion.setId("1");

        Sample sampleAfterPropertyMerge = new Sample();
        sampleAfterPropertyMerge.setAlias("test1");
        sampleAfterPropertyMerge.setTaxonId(7L);
        sampleAfterPropertyMerge.setId("1");

        Row row = new Row();

        when(sampleRepository.findOneBySubmissionIdAndAlias(submission.getId(), s.getAlias()))
                .thenReturn(storedVersion);

        sheetLoaderService.storeSubmittable(submission, row, s);

        verify(sampleRepository).findOneBySubmissionIdAndAlias(submission.getId(), s.getAlias());
        verify(publisher).publishEvent(any(BeforeSaveEvent.class));
        verify(sampleRepository).save(sampleAfterPropertyMerge);
    }


    private Sheet sheet(Submission submission) {
        Sheet sheet = new Sheet();

        sheet.setSubmission(submission);
        sheet.setTemplate(template());

        sheet.setVersion(0L);

        sheet.setHeaderRow(new Row(new String[]{"unique name", "title", "description", "taxon", "taxon id", "height", "units"}));
        sheet.addRow(new String[]{
                "s1", "", "", "Homo sapiens", "9606", "1.7", "meters"
        });
        sheet.addRow(new String[]{
                "s2", "", "", "Homo sapiens", "9606", "1.7", "meters"
        });

        sheet.setStatus(SheetStatusEnum.Submitted);

        return sheet;
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
        return template;
    }


    private JSONObject stringToJsonObject(String jsonContent) {
        return new JSONObject(jsonContent);
    }

}
