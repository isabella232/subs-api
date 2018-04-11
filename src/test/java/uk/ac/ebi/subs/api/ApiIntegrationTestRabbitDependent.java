package uk.ac.ebi.subs.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.RabbitMQDependentTest;
import uk.ac.ebi.subs.api.processors.SubmissionStatusResourceProcessor;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.Profile;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileRepositoryRest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Category(RabbitMQDependentTest.class)
@ActiveProfiles("basic_auth")
public class ApiIntegrationTestRabbitDependent {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @LocalServerPort
    private int port;
    private String rootUri;

    private ApiIntegrationTestHelper testHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    SubmissionStatusRepository submissionStatusRepository;

    @Autowired
    private SampleRepository sampleRepository;

    @MockBean
    private DomainService domainService;
    @MockBean
    private ProfileRepositoryRest profileRepositoryRest;


    @Before
    public void buildUp() throws URISyntaxException {
        rootUri = "http://localhost:" + port + "/api";
        final Map<String, String> standardGetContentHeader = ApiIntegrationTestHelper.createStandardGetHeader();
        standardGetContentHeader.putAll(ApiIntegrationTestHelper.createBasicAuthheaders(TestWebSecurityConfig.USI_USER, TestWebSecurityConfig.USI_PASSWORD));
        final Map<String, String> standardPostContentHeader = ApiIntegrationTestHelper.createStandardGetHeader();
        standardPostContentHeader.putAll(ApiIntegrationTestHelper.createBasicAuthheaders(TestWebSecurityConfig.USI_USER, TestWebSecurityConfig.USI_PASSWORD));
        testHelper = new ApiIntegrationTestHelper(objectMapper, rootUri,
                Arrays.asList(submissionRepository, sampleRepository, submissionStatusRepository), standardGetContentHeader, standardPostContentHeader);

        ApiIntegrationTestHelper.mockAapProfileAndDomain(domainService,profileRepositoryRest);

    }

    @After
    public void tearDown() throws IOException {
        submissionRepository.deleteAll();
        sampleRepository.deleteAll();
        submissionStatusRepository.deleteAll();
    }

    @Test
    @Category(RabbitMQDependentTest.class)
    //Requires dispatcher to delete the contents
    public void postThenDeleteSubmission() throws UnirestException, IOException {
        Map<String, String> rootRels = testHelper.rootRels();

        String submissionLocation = testHelper.submissionWithSamples(rootRels);
        HttpResponse<JsonNode> deleteResponse = Unirest.delete(submissionLocation)
                .headers(testHelper.getPostHeaders())
                .asJson();

        assertThat(deleteResponse.getStatus(), equalTo(HttpStatus.NO_CONTENT.value()));

        List<Submission> submissions = submissionRepository.findAll();
        assertThat(submissions, empty());
    }

    /**
     * create a submission with some samples and submit it
     *
     * @throws IOException
     * @throws UnirestException
     */
    @Test
    @Category(RabbitMQDependentTest.class)
    public void simpleSubmissionWorkflow() throws IOException, UnirestException {
        Map<String, String> rootRels = testHelper.rootRels();

        String submissionLocation = testHelper.submissionWithSamples(rootRels);

        HttpResponse<JsonNode> submissionGetResponse = Unirest
                .get(submissionLocation)
                .headers(testHelper.getGetHeaders())
                .asJson();

        assertThat(submissionGetResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));
        JSONObject payload = submissionGetResponse.getBody().getObject();

        Map<String, String> rels = testHelper.relsFromPayload(payload);

        assertThat(rels.get("submissionStatus"), notNullValue());
        String submissionStatusLocation = rels.get("submissionStatus");

        HttpResponse<JsonNode> submissionStatusGetResponse = Unirest
                .get(submissionStatusLocation)
                .headers(testHelper.getGetHeaders())
                .asJson();

        assertThat(submissionStatusGetResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));
        JSONObject statusPayload = submissionStatusGetResponse.getBody().getObject();

        assertThat(statusPayload.getJSONObject("_links").has(SubmissionStatusResourceProcessor.AVAILABLE_STATUSES_REL),
                is(equalTo(true)));

        rels = testHelper.relsFromPayload(statusPayload);

        assertThat(rels.get("self"), notNullValue());
        submissionStatusLocation = rels.get("self");

        //update the submission
        //create a new submission
        HttpResponse<JsonNode> submissionPatchResponse = Unirest.patch(submissionStatusLocation)
                .headers(testHelper.getPostHeaders())
                .body("{\"status\": \"Submitted\"}")
                .asJson();

        assertThat(submissionPatchResponse.getStatus(), is(equalTo(HttpStatus.BAD_REQUEST.value()))); //validation results required
    }


}
