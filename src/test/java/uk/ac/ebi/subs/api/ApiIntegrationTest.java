package uk.ac.ebi.subs.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import uk.ac.ebi.subs.api.controllers.SubmissionDTO;
import uk.ac.ebi.subs.api.error.ApiError;
import uk.ac.ebi.subs.data.client.Sample;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.SubmissionPlan;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.model.templates.AttributeCapture;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.JsonFieldType;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionPlanRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;
import uk.ac.ebi.subs.repository.services.SubmittableHelperService;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileRepositoryRest;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.subs.api.Helpers.generateTestSamples;
import static uk.ac.ebi.subs.api.utils.SubmittableHelper.generateTestTeam;

@WithMockUser(username = "usi_admin_user", roles = {Helpers.ADMIN_TEAM_NAME})
public abstract class ApiIntegrationTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @LocalServerPort
    private int port;
    protected String rootUri;

    protected ApiIntegrationTestHelper testHelper;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected SubmissionRepository submissionRepository;
    @Autowired
    protected SubmissionPlanRepository submissionPlanRepository;
    @Autowired
    protected SubmissionStatusRepository submissionStatusRepository;
    @Autowired
    protected SampleRepository sampleRepository;
    @Autowired
    protected ChecklistRepository checklistRepository;
    @Autowired
    protected ValidationResultRepository validationResultRepository;
    @Autowired
    protected ProcessingStatusRepository processingStatusRepository;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @MockBean
    private DomainService domainService;

    @MockBean
    private ProfileRepositoryRest profileRepositoryRest;


    @Autowired
    private SubmittableHelperService submittableHelperService;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    private DataType sampleDataType;

    @Before
    public void buildUp() throws UnirestException {
        clearDbs();

        rootUri = "http://localhost:" + port + "/api";
        testHelper = new ApiIntegrationTestHelper(objectMapper, rootUri,
                Arrays.asList(submissionRepository, sampleRepository, submissionStatusRepository), createGetHeaders(), createPostHeaders());

        ApiIntegrationTestHelper.mockAapProfileAndDomain(domainService, profileRepositoryRest);
        List<DataType> dataTypes = ApiIntegrationTestHelper.initialiseDataTypes(dataTypeRepository);

        sampleDataType = dataTypes.stream().filter(dt -> dt.getId().equals("samples")).findAny().get();

    }

    public void clearDbs() {
        Arrays.asList(
                submissionRepository,
                submissionStatusRepository,
                sampleRepository,
                checklistRepository,
                validationResultRepository,
                processingStatusRepository,
                dataTypeRepository,
                submissionPlanRepository
        ).forEach(CrudRepository::deleteAll);
    }

    @After
    public void tearDown() throws IOException {
        clearDbs();
    }

    @Test
    public void checkRootRels() throws UnirestException, IOException {
        Map<String, String> rootRels = testHelper.rootRels();

        assertThat(rootRels.keySet(), hasItems("userTeams", "team"));
        assertThat(rootRels.keySet(), not(hasItems("submissions:create", "samples:create")));
    }

    @Test
    public void downloadTemplate() throws UnirestException, IOException {
        Map<String, String> rootRels = testHelper.rootRels();
        assertThat(rootRels.keySet(), hasItems("checklists"));

        Checklist checklist = new Checklist();
        checklist.setId("bar");
        Template template = new Template();
        template
                .add(
                        "alias",
                        FieldCapture.builder().fieldName("alias").build()
                )
                .add(
                        "taxon id",
                        FieldCapture.builder().fieldName("taxonId").fieldType(JsonFieldType.IntegerNumber).build()
                )
                .add(
                        "taxon",
                        FieldCapture.builder().fieldName("taxon").build()
                );

        template.setDefaultCapture(
                AttributeCapture.builder().build()
        );

        checklist.setSpreadsheetTemplate(template);

        checklistRepository.insert(checklist);

        HttpResponse<JsonNode> templatesResponse = Unirest.get(rootRels.get("checklists")).headers(testHelper.getGetHeaders()).asJson();

        JSONArray templates = templatesResponse.getBody().getObject().getJSONObject("_embedded").getJSONArray("checklists");
        JSONObject exampleTemplateJson = templates.getJSONObject(0);
        String spreadsheetLink = exampleTemplateJson
                .getJSONObject("_links")
                .getJSONObject("spreadsheet-csv-download")
                .getString("href");

        Map<String, String> requestHeaders = testHelper.getGetHeaders();
        requestHeaders.put("Accept", "text/csv");

        HttpResponse<String> templateResponse = Unirest.get(spreadsheetLink).headers(requestHeaders).asString();
        Headers responseHeaders = templateResponse.getHeaders();

        assertThat(responseHeaders.getFirst("Content-Disposition"), equalTo("attachment; filename=\"bar_template.csv\""));
    }

    @Test
    public void postSubmission() throws UnirestException, IOException {
        Map<String, String> rootRels = testHelper.rootRels();

        SubmissionPlan submissionPlan = Helpers.generateSubmissionPlan();
        submissionPlanRepository.insert(submissionPlan);

        SubmissionDTO submissionDTO = Helpers.generateSubmissionDTO(rootUri, submissionPlan);
        testHelper.postSubmission(rootRels, submissionDTO);

        List<SubmissionStatus> submissionStatuses = submissionStatusRepository.findAll();
        assertThat(submissionStatuses, notNullValue());
        assertThat(submissionStatuses, hasSize(1));
        SubmissionStatus submissionStatus = submissionStatuses.get(0);
        assertThat(submissionStatus.getStatus(), notNullValue());
        assertThat(submissionStatus.getStatus(), equalTo("Draft"));
    }

    /**
     * POSTing two samples with the same alias in one submission should throw an error
     */
    @Test
    public void reuseAliasInSubmissionGivesError() throws IOException, UnirestException {
        Map<String, String> rootRels = testHelper.rootRels();

        SubmissionPlan submissionPlan = Helpers.generateSubmissionPlan();
        submissionPlanRepository.insert(submissionPlan);

        SubmissionDTO submissionDTO = Helpers.generateSubmissionDTO(rootUri, submissionPlan);
        List<Sample> testSamples = Helpers.generateTestClientSamples(1);
        Sample sample = testSamples.get(0);


        HttpResponse<JsonNode> submissionResponse = testHelper.postSubmission(rootRels, submissionDTO);

        String submissionLocation = submissionResponse.getHeaders().getFirst("Location");
        Map<String, String> submissionRels = testHelper.relsFromPayload(submissionResponse.getBody().getObject());
        Map<String, String> submissionContentsRels = testHelper.relsFromUri(submissionRels.get("contents"));

        assertThat(submissionContentsRels.get("samples:create"), notNullValue());

        sample.setSubmission(submissionLocation);

        HttpResponse<JsonNode> sampleFirstResponse = Unirest.post(submissionContentsRels.get("samples:create"))
                .headers(testHelper.getPostHeaders())
                .body(sample)
                .asJson();

        assertThat(sampleFirstResponse.getStatus(), is(equalTo(HttpStatus.CREATED.value())));

        HttpResponse<JsonNode> sampleSecondResponse = Unirest.post(submissionContentsRels.get("samples:create"))
                .headers(testHelper.getPostHeaders())
                .body(sample)
                .asJson();

        assertThat(sampleSecondResponse.getStatus(), is(equalTo(HttpStatus.BAD_REQUEST.value())));

        ObjectMapper mapper = new ObjectMapper();
        ApiError apiErrorResponse = mapper.readValue(sampleSecondResponse.getBody().toString(), ApiError.class);

        List<String> errors = apiErrorResponse.getErrors();
        assertThat(errors, notNullValue());

        Optional<String> optionalError = errors.stream()
                .filter(error -> error.contains("already_exists: In Sample, field alias can't be "))
                .findAny();

        assertTrue(optionalError.isPresent());

        assertEquals(HttpStatus.BAD_REQUEST.value(), apiErrorResponse.getStatus());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), apiErrorResponse.getTitle());
    }

    /**
     * POSTing a sample with no alias should throw an error
     */
    @Test
    public void postSampleWithNoAlias() throws IOException, UnirestException {
        Map<String, String> rootRels = testHelper.rootRels();

        SubmissionPlan submissionPlan = Helpers.generateSubmissionPlan();
        submissionPlanRepository.insert(submissionPlan);

        SubmissionDTO submissionDTO = Helpers.generateSubmissionDTO(rootUri, submissionPlan);

        Sample sample = Helpers.generateTestClientSamples(1).get(0);
        sample.setAlias(null);

        HttpResponse<JsonNode> submissionResponse = testHelper.postSubmission(rootRels, submissionDTO);

        Map<String, String> submissionRels = testHelper.relsFromPayload(submissionResponse.getBody().getObject());
        Map<String, String> submissionContentsRels = testHelper.relsFromUri(submissionRels.get("contents"));

        assertThat(submissionContentsRels.get("samples:create"), notNullValue());

        HttpResponse<JsonNode> samplePostResponse = Unirest.post(submissionContentsRels.get("samples:create"))
                .headers(testHelper.getPostHeaders())
                .body(sample)
                .asJson();

        assertThat(samplePostResponse.getStatus(), equalTo(HttpStatus.BAD_REQUEST.value()));

    }

    /**
     * POSTing two samples with different aliases in one submission, and changing one so they have the same
     * alias should throw an error
     */
    @Test
    public void sneakyReuseAliasInSubmissionGivesError() throws IOException, UnirestException {
        Map<String, String> rootRels = testHelper.rootRels();

        SubmissionPlan submissionPlan = Helpers.generateSubmissionPlan();
        submissionPlanRepository.insert(submissionPlan);

        SubmissionDTO submissionDTO = Helpers.generateSubmissionDTO(rootUri, submissionPlan);
        List<Sample> testSamples = Helpers.generateTestClientSamples(2);
        Map<Sample, String> testSampleLocations = new HashMap<>();

        HttpResponse<JsonNode> submissionResponse = testHelper.postSubmission(rootRels, submissionDTO);

        String submissionLocation = submissionResponse.getHeaders().getFirst("Location");
        Map<String, String> submissionRels = testHelper.relsFromPayload(submissionResponse.getBody().getObject());
        Map<String, String> submissionContentsRels = testHelper.relsFromUri(submissionRels.get("contents"));

        assertThat(submissionContentsRels.get("samples:create"), notNullValue());

        for (Sample sample : testSamples) {

            // sample.setSubmission(submissionLocation);

            HttpResponse<JsonNode> samplePostResponse = Unirest.post(submissionContentsRels.get("samples:create"))
                    .headers(testHelper.getPostHeaders())
                    .body(sample)
                    .asJson();

            assertThat(samplePostResponse.getStatus(), equalTo(HttpStatus.CREATED.value()));

            testSampleLocations.put(sample, samplePostResponse.getHeaders().getFirst("Location"));
        }

        Sample firstSample = testSamples.remove(0);

        for (Sample sample : testSamples) {
            String sampleLocation = testSampleLocations.get(sample);

            sample.setAlias(firstSample.getAlias());

            HttpResponse<JsonNode> samplePutResponse = Unirest.put(sampleLocation)
                    .headers(testHelper.getPostHeaders())
                    .body(sample)
                    .asJson();

            assertThat(samplePutResponse.getStatus(), equalTo(HttpStatus.BAD_REQUEST.value()));

            ObjectMapper mapper = new ObjectMapper();
            ApiError apiErrorResponse = mapper.readValue(samplePutResponse.getBody().toString(), ApiError.class);

            List<String> errors = apiErrorResponse.getErrors();

            assertThat(errors, notNullValue());

            Optional<String> optionalError = errors.stream()
                    .filter(error -> error.contains("already_exists: In Sample, field alias can't be "))
                    .findAny();

            assertTrue(optionalError.isPresent());

            assertEquals(HttpStatus.BAD_REQUEST.value(), apiErrorResponse.getStatus());
            assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), apiErrorResponse.getTitle());
        }
    }

    /**
     * Make multiple submissions with the same contents. Use the sample history endpoint to check that you can
     * get the right number of entries back
     *
     * @throws IOException
     * @throws UnirestException
     */
    @Test
    public void sampleVersions() throws IOException, UnirestException {
        uk.ac.ebi.subs.repository.model.Submission submission;
        Team testTeam = generateTestTeam();

        int numberOfSubmissions = 5;
        int numberOfSamples = 2;

        // At this point we are bypassing our API validation checks and inserting the objects directly into the DB,
        // this is for test purposes only and the DB must be cleared afterwards.
        for (int i = 0; i < numberOfSubmissions; i++) {
            submission = Helpers.generateSubmission();
            submission.setTeam(testTeam);

            SubmissionStatus submissionStatus = new SubmissionStatus(SubmissionStatusEnum.Draft);
            submission.setSubmissionStatus(submissionStatus);
            submissionStatusRepository.insert(submissionStatus);

            submissionRepository.insert(submission);

            submissionPlanRepository.insert(submission.getSubmissionPlan());

            HttpResponse<JsonNode> submissionResponse = Unirest.get(rootUri + "/submissions/" + submission.getId())
                    .headers(testHelper.getGetHeaders()).asJson();
            assertEquals(HttpStatus.OK.value(), submissionResponse.getStatus());

            for (uk.ac.ebi.subs.repository.model.Sample sample : generateTestSamples(numberOfSamples, false)) {
                sample.setSubmission(submission);
                sample.setDataType(sampleDataType);
                submittableHelperService.uuidAndTeamFromSubmissionSetUp(sample);
                submittableHelperService.processingStatusAndValidationResultSetUpForSubmittable(sample);
                sampleRepository.save(sample);

                HttpResponse<JsonNode> sampleResponse = Unirest.get(rootUri + "/samples/" + sample.getId())
                        .headers(testHelper.getGetHeaders()).asJson();
                assertEquals(HttpStatus.OK.value(), sampleResponse.getStatus());
            }
        }

        String teamName = Helpers.TEAM_NAME;
        String teamUrl = this.rootUri + "/teams/" + teamName;
        HttpResponse<JsonNode> teamQueryResponse = Unirest.get(teamUrl).headers(testHelper.getGetHeaders()).asJson();

        assertThat(teamQueryResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));

        JSONObject teamPayload = teamQueryResponse.getBody().getObject();
        Map<String, String> teamRels = testHelper.relsFromPayload(teamPayload);
        Map<String, String> teamContentsRels = testHelper.relsFromUri(teamRels.get("items"));
        String teamSamplesUrl = teamContentsRels.get("samples");

        assertThat(teamSamplesUrl, notNullValue());

        HttpResponse<JsonNode> teamSamplesQueryResponse = Unirest.get(teamSamplesUrl).headers(testHelper.getGetHeaders()).asJson();
        assertThat(teamSamplesQueryResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));
        JSONObject teamSamplesPayload = teamSamplesQueryResponse.getBody().getObject();
        JSONArray teamSamples = teamSamplesPayload.getJSONObject("_embedded").getJSONArray("samples");

        assertThat(teamSamples.length(), is(equalTo(numberOfSamples)));

        for (int i = 0; i < teamSamples.length(); i++) {
            JSONObject teamSample = teamSamples.getJSONObject(i);

            Map<String, String> sampleRels = testHelper.relsFromPayload(teamSample);
            String selfUrl = sampleRels.get("self");

            HttpResponse<JsonNode> sampleResponse = Unirest.get(selfUrl).headers(testHelper.getGetHeaders()).asJson();
            assertThat(sampleResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));
            JSONObject samplePayload = sampleResponse.getBody().getObject();
            sampleRels = testHelper.relsFromPayload(samplePayload);

            String historyUrl = sampleRels.get("history");

            assertThat(historyUrl, notNullValue());

            HttpResponse<JsonNode> historyResponse = Unirest.get(historyUrl).headers(testHelper.getGetHeaders()).asJson();
            assertThat(historyResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));
            JSONObject historyPayload = historyResponse.getBody().getObject();
            assertThat(historyPayload.has("_embedded"), is(true));
            JSONObject embedded = historyPayload.getJSONObject("_embedded");
            assertThat(embedded.has("samples"), is(true));
            JSONArray sampleHistory = embedded.getJSONArray("samples");
            assertThat(sampleHistory.length(), is(equalTo(numberOfSubmissions)));
        }
    }

    @Test
    public void testPut() throws IOException, UnirestException {
        SubmissionPlan submissionPlan = Helpers.generateSubmissionPlan();
        submissionPlanRepository.insert(submissionPlan);

        SubmissionDTO submissionDTO = Helpers.generateSubmissionDTO(rootUri, submissionPlan);
        Map<String, String> teamRels = testHelper.teamRels(Helpers.TEAM_NAME);

        HttpResponse<JsonNode> submissionResponse = testHelper.postSubmission(teamRels, submissionDTO);

        String submissionLocation = submissionResponse.getHeaders().getFirst("Location");
        Map<String, String> submissionRels = testHelper.relsFromPayload(submissionResponse.getBody().getObject());

        Map<String, String> submissionContentsRels = testHelper.relsFromUri(submissionRels.get("contents"));


        assertThat(submissionContentsRels.get("samples"), notNullValue());

        Sample sample = Helpers.generateTestClientSamples(1).get(0);
        //add samples to the submission

        sample.setSubmission(submissionLocation);

        HttpResponse<JsonNode> sampleResponse = Unirest.post(submissionContentsRels.get("samples:create"))
                .headers(testHelper.getPostHeaders())
                .body(sample)
                .asJson();

        assertThat(sampleResponse.getStatus(), is(equalTo(HttpStatus.CREATED.value())));
        assertThat(sampleResponse.getHeaders().getFirst("Location"), notNullValue());

        String sampleLocation = sampleResponse.getHeaders().getFirst("Location");

        sample.setAlias("bob"); //modify the sample
        sample.setSubmission(submissionLocation);

        HttpResponse<JsonNode> samplePutResponse = Unirest.put(sampleLocation)
                .headers(testHelper.getPostHeaders())
                .body(sample)
                .asJson();

        logger.info("samplePutResponse: {}", samplePutResponse.getBody());
        assertThat(samplePutResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));
    }

    public Map<String, String> createGetHeaders() throws UnirestException {
        return ApiIntegrationTestHelper.createStandardGetHeader();
    }

    public Map<String, String> createPostHeaders() throws UnirestException {
        return ApiIntegrationTestHelper.createStandardPostHeader();
    }
}
