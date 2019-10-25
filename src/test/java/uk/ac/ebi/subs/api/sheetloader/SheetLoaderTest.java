package uk.ac.ebi.subs.api.sheetloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.api.services.SubmittableValidationDispatcher;
import uk.ac.ebi.subs.data.component.Attribute;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.model.templates.AttributeCapture;
import uk.ac.ebi.subs.repository.model.templates.Capture;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.JsonFieldType;
import uk.ac.ebi.subs.repository.model.templates.NoOpCapture;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.SpreadsheetRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
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
    private SpreadsheetRepository spreadsheetRepository;
    @MockBean
    private SampleRepository sampleRepository;
    @MockBean
    private DataTypeRepository dataTypeRepository;
    @MockBean
    private ChecklistRepository checklistRepository;
    @MockBean
    private SubmissionRepository submissionRepository;

    private Submission submission;
    private Checklist checklist;
    private DataType dataType;

    @Before
    public void setUp() {
        objectMapper.registerModule(new JavaTimeModule());

        Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>>
                submittableRepositoryMap = new HashMap<>();
        submittableRepositoryMap.put(Sample.class, sampleRepository);

        Map<String, Class<? extends StoredSubmittable>> submittablesByCollectionName = new HashMap<>();
        submittablesByCollectionName.put("samples", Sample.class);


        this.sheetLoaderService = new SheetLoaderService(
                submittableRepositoryMap,
                objectMapper,
                spreadsheetRepository,
                submittableValidationDispatcher,
                sheetBulkOps,
                dataTypeRepository,
                checklistRepository,
                submissionRepository
        );

        this.dataType = new DataType();
        this.dataType.setId("foo");
        this.dataType.setSubmittableClassName(Sample.class.getName());

        this.checklist = new Checklist();
        this.checklist.setId("bar");
        this.checklist.setDataTypeId(dataType.getId());
        this.checklist.setSpreadsheetTemplate(template());

        this.submission = new Submission();
        this.submission.setTeam(Team.build("test"));
        this.submission.setId("1234");
        this.sheet = sheet(submission, checklist);

        Template template = template();
        Map<String, Capture> captureMap = template.getColumnCaptures();

        expectedCaptures = Arrays.asList(
                captureMap.get("unique name"),
                captureMap.get("title"),
                captureMap.get("description"),
                captureMap.get("taxon"),
                captureMap.get("taxon id"),
                AttributeCapture.builder().build(),
                NoOpCapture.builder().build(),
                captureMap.get("release date")
        );


        Mockito.when(dataTypeRepository.findOne(dataType.getId())).thenReturn(dataType);
        Mockito.when(checklistRepository.findOne(checklist.getId())).thenReturn(checklist);
        Mockito.when(submissionRepository.findOne(submission.getId())).thenReturn(submission);
    }

    private Spreadsheet sheet;
    private List<Capture> expectedCaptures;


    @Test
    public void map_headings_to_column_captures() {
        Template template = checklist.getSpreadsheetTemplate();

        List<Capture> actualColumnMappings = sheetLoaderService.mapColumns(
                sheet.getHeaderRow(),
                template.getColumnCaptures(),
                Optional.of(template.getDefaultCapture())
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
                        "  \"releaseDate\": \"2018-10-04\",\n" +
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
                        "  \"releaseDate\": \"2018-10-04\",\n" +
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
        expectedSample.setSubmission(submission);
        expectedSample.setTeam(submission.getTeam());
        expectedSample.setChecklist(checklist);
        expectedSample.setDataType(dataType);

        Sample actualSample = (Sample) sheetLoaderService.documentToSubmittable(
                Sample.class,
                submission,
                sheet.getRows().get(0),
                json,
                dataType,
                checklist
        );

        assertThat(actualSample, samePropertyValuesAs(expectedSample));
    }

    @Test
    public void convert_sheet_to_submittables() {
        List<Pair<Row, ? extends StoredSubmittable>> expected = new ArrayList<>(submittablesWithPairs());

        List<Pair<Row, ? extends StoredSubmittable>> actual = new ArrayList<>(
                sheetLoaderService.convertToSubmittables(
                    sheet, Sample.class, checklist.getSpreadsheetTemplate(), submission, dataType, checklist));

        for (int i = 0; i < expected.size(); i++) {
            assertThat(actual.get(i).getSecond(), samePropertyValuesAs(expected.get(i).getSecond()));
        }
    }

    @Test
    public void convert_sheet_to_submittables_without_alias() {

        //clear all alias fields, should not get converted to submittables
        sheet.getRows().forEach(r -> r.getCells().set(0, ""));

        List<Pair<Row, ? extends StoredSubmittable>> actual = sheetLoaderService.convertToSubmittables(
                sheet, Sample.class, checklist.getSpreadsheetTemplate(), submission, dataType, checklist);

        Assert.assertTrue(actual.isEmpty());

        for (Row row : sheet.getRows()) {
            Assert.assertTrue(row.hasErrors());
        }


    }

    private List<Pair<Row, ? extends StoredSubmittable>> submittablesWithPairs() {
        List<Pair<Row, ? extends StoredSubmittable>> pairs = Arrays.asList(
                Pair.of(sheet.getRows().get(0), sample("s1")),
                Pair.of(sheet.getRows().get(1), sample("s2"))
        );

        for (Pair<Row, ? extends StoredSubmittable> p : pairs) {
            p.getSecond().setTeam(submission.getTeam());
            p.getSecond().setSubmission(submission);
            p.getSecond().setChecklist(checklist);
            p.getSecond().setDataType(dataType);
        }
        return pairs;
    }


    @Test
    public void load_sheet() {
        List<Pair<Row, ? extends StoredSubmittable>> submittablesWithPairs = submittablesWithPairs();

        Answer<Collection<Pair<Row, ? extends StoredSubmittable>>> ans = invocation -> {
            submittablesWithPairs.get(0).getSecond().setId("ID");
            return submittablesWithPairs;
        };

        when(sheetBulkOps.lookupExistingEntries(submission,
                submittablesWithPairs,
                sampleRepository)).thenAnswer(ans);

        List<Pair<Row, ? extends StoredSubmittable>> existingSubmittables = submittablesWithPairs.subList(0, 1);
        List<Pair<Row, ? extends StoredSubmittable>> freshSubmittables = submittablesWithPairs.subList(1, 2);

        sheetLoaderService.loadSheet(sheet);

        verify(sheetBulkOps).lookupExistingEntries(org.mockito.Matchers.eq(submission),
                org.mockito.Matchers.anyCollection(),
                org.mockito.Matchers.eq(sampleRepository));

        verify(sheetBulkOps).updateExistingSubmittables(existingSubmittables, sampleRepository);

        verify(sheetBulkOps).insertNewSubmittables(freshSubmittables, sampleRepository);

        verify(submittableValidationDispatcher).validateUpdate(submittablesWithPairs.get(0).getSecond());

        verify(spreadsheetRepository, times(2)).save(sheet);
        assertEquals(SheetStatusEnum.Completed, sheet.getStatus());
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


    private Spreadsheet sheet(Submission submission, Checklist checklist) {
        Spreadsheet sheet = new Spreadsheet();

        sheet.setSubmissionId(submission.getId());
        sheet.setChecklistId(checklist.getId());
        sheet.setDataTypeId(checklist.getDataTypeId());

        sheet.setVersion(0L);

        sheet.setHeaderRow(new Row(new String[]{"unique name", "title", "description", "taxon", "taxon id", "height", "units", "release date"}));
        sheet.addRow(new String[]{
                "s1", "", "", "Homo sapiens", "9606", "1.7", "meters", "2018-10-04"
        });
        sheet.addRow(new String[]{
                "s2", "", "", "Homo sapiens", "9606", "1.7", "meters", "2018-10-04"
        });

        sheet.setStatus(SheetStatusEnum.Submitted);

        return sheet;
    }

    private Sample sample(String alias) {
        Sample sample = new Sample();
        sample.setAlias(alias);
        sample.setTaxon("Homo sapiens");
        sample.setTaxonId(9606L);
        Attribute heightAttribute = new Attribute();
        heightAttribute.setValue("1.7");
        heightAttribute.setUnits("meters");
        sample.getAttributes().put("height", Arrays.asList(heightAttribute));
        sample.setReleaseDate(LocalDate.of(2018, 10, 4));
        sample.setReferences(new HashMap());
        return sample;
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
                .add(
                        "release date",
                        FieldCapture.builder().fieldName("releaseDate").build()
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
