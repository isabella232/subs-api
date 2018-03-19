package uk.ac.ebi.subs.api.sheetloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.util.Pair;
import org.springframework.hateoas.RelProvider;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.api.services.SubmittableValidationDispatcher;
import uk.ac.ebi.subs.data.component.Attribute;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class SheetLoaderTest {


    private SheetLoaderService sheetLoaderService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private SubmittableValidationDispatcher submittableValidationDispatcher;

    @MockBean
    private SheetBulkOps sheetBulkOps;

    @MockBean
    private SheetRepository sheetRepository;

    @MockBean
    private SampleRepository sampleRepository;

    @MockBean
    private RelProvider relProvider;

    private Submission submission;

    @Before
    public void setUp() {
        Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>>
                submittableRepositoryMap = new HashMap<>();
        submittableRepositoryMap.put(Sample.class, sampleRepository);


        Mockito.doReturn("samples").when(relProvider).getCollectionResourceRelFor(any());

        sheetLoaderService = new SheetLoaderService(
                submittableRepositoryMap,
                sheetRepository,
                relProvider,
                objectMapper,
                submittableValidationDispatcher,
                sheetBulkOps
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



    }

    private Sheet sheet;
    private List<Capture> expectedCaptures;


    @Test
    public void map_headings_to_column_captures() {

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
    public void convert_document_to_sample() {
        JSONObject json = stringToJsonObject(
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


        Sample expectedSample = sample("s1");

        Sample actualSample = (Sample) sheetLoaderService.documentToSubmittable(
                Sample.class,
                sheet.getSubmission(),
                sheet.getRows().get(0),
                json
        );

        Assert.assertEquals(expectedSample, actualSample);

    }

    @Test
    public void convert_sheet_to_submittables() {
        List<Pair<Row, ? extends StoredSubmittable>> expected = submittablesWithPairs();

        List<Pair<Row, ? extends StoredSubmittable>> actual = sheetLoaderService.convertToSubmittables(sheet, Sample.class);

        Assert.assertEquals(expected, actual);
    }

    private List<Pair<Row, ? extends StoredSubmittable>> submittablesWithPairs() {
        return Arrays.asList(
                Pair.of(sheet.getRows().get(0), sample("s1")),
                Pair.of(sheet.getRows().get(1), sample("s2"))
        );
    }


    @Test
    public void load_sheet() {
        List<Pair<Row, ? extends StoredSubmittable>> submittablesWithPairs = submittablesWithPairs();

        Answer<Collection<Pair<Row, ? extends StoredSubmittable>>> ans = invocation -> {
            submittablesWithPairs.get(0).getSecond().setId("ID");
            return submittablesWithPairs;
        };

        when(sheetBulkOps.lookupExistingEntries(sheet.getSubmission(),
                submittablesWithPairs,
                sampleRepository)).thenAnswer(ans);

        List<Pair<Row, ? extends StoredSubmittable>> existingSubmittables = submittablesWithPairs.subList(0, 1);
        List<Pair<Row, ? extends StoredSubmittable>> freshSubmittables = submittablesWithPairs.subList(1, 2);

        sheetLoaderService.loadSheet(sheet);

        verify(sheetBulkOps).lookupExistingEntries(org.mockito.Matchers.eq(sheet.getSubmission()),
                org.mockito.Matchers.anyCollection(),
                org.mockito.Matchers.eq(sampleRepository));

        verify(sheetBulkOps).updateExistingSubmittables(existingSubmittables, sampleRepository);

        verify(sheetBulkOps).insertNewSubmittables(freshSubmittables, sampleRepository);

        verify(submittableValidationDispatcher).validateUpdate(submittablesWithPairs.get(0).getSecond());

        verify(sheetRepository).save(sheet);
        assertEquals(SheetStatusEnum.Completed,sheet.getStatus());

    }


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

    private Sample sample(String alias) {
        Sample sample = new Sample();
        sample.setAlias(alias);
        sample.setTaxon("Homo sapiens");
        sample.setTaxonId(9606L);
        sample.setDescription("");
        sample.setTitle("");
        Attribute heightAttribute = new Attribute();
        heightAttribute.setValue("1.7");
        heightAttribute.setUnits("meters");
        sample.getAttributes().put("height", Arrays.asList(heightAttribute));
        return sample;
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
