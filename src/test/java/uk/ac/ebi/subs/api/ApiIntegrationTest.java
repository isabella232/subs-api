package uk.ac.ebi.subs.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.RabbitMQDependentTest;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.client.Sample;
import uk.ac.ebi.subs.repository.SubmissionRepository;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SampleRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionStatusRepository;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiIntegrationTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @LocalServerPort
    private int port;
    private String rootUri;


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    SubmissionStatusRepository submissionStatusRepository;

    @Autowired
    private SampleRepository sampleRepository;


    @Before
    public void buildUp() throws URISyntaxException {
        rootUri = "http://localhost:" + port + "/api";
        submissionRepository.deleteAll();
        sampleRepository.deleteAll();
        submissionStatusRepository.deleteAll();

        Unirest.setObjectMapper(new com.mashape.unirest.http.ObjectMapper() {
            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return objectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return objectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @After
    public void tearDown() throws IOException {
        Unirest.shutdown();
        submissionRepository.deleteAll();
        sampleRepository.deleteAll();
        submissionStatusRepository.deleteAll();
    }

    @Test
    public void checkRootRels() throws UnirestException, IOException {
        Map<String, String> rootRels = rootRels();

        assertThat(rootRels.keySet(), hasItems("submissions", "samples"));
    }

    @Test
    public void postSubmission() throws UnirestException, IOException {
        Map<String, String> rootRels = rootRels();

        Submission submission = Helpers.generateSubmission();
        HttpResponse<JsonNode> submissionResponse = postSubmission(rootRels, submission);

        List<SubmissionStatus> submissionStatuses = submissionStatusRepository.findAll();
        assertThat(submissionStatuses, notNullValue());
        assertThat(submissionStatuses, hasSize(1));
        SubmissionStatus submissionStatus = submissionStatuses.get(0);
        assertThat(submissionStatus.getStatus(), notNullValue());
        assertThat(submissionStatus.getStatus(), equalTo("Draft"));
    }

    @Test
    @Category(RabbitMQDependentTest.class)
    //Requires dispatcher to delete the contents
    public void postThenDeleteSubmission() throws UnirestException, IOException {
        Map<String, String> rootRels = rootRels();

        String submissionLocation = submissionWithSamples(rootRels);
        HttpResponse<JsonNode> deleteResponse = Unirest.delete(submissionLocation)
                .headers(standardPostHeaders())
                .asJson();

        assertThat(deleteResponse.getStatus(), equalTo(HttpStatus.NO_CONTENT.value()));

        List<uk.ac.ebi.subs.repository.model.Submission> submissions = submissionRepository.findAll();
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
        Map<String, String> rootRels = rootRels();

        String submissionLocation = submissionWithSamples(rootRels);

        HttpResponse<JsonNode> submissionGetResponse = Unirest
                .get(submissionLocation)
                .headers(standardGetHeaders())
                .asJson();

        assertThat(submissionGetResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));
        JSONObject payload = submissionGetResponse.getBody().getObject();

        Map<String,String> rels = relsFromPayload(payload);

        assertThat(rels.get("submissionStatus"),notNullValue());
        String submissionStatusLocation = rels.get("submissionStatus");

        HttpResponse<JsonNode> submissionStatusGetResponse = Unirest
                .get(submissionStatusLocation)
                .headers(standardGetHeaders())
                .asJson();

        assertThat(submissionStatusGetResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));
        JSONObject statusPayload = submissionStatusGetResponse.getBody().getObject();

        rels = relsFromPayload(statusPayload);

        assertThat(rels.get("self"),notNullValue());
        submissionStatusLocation = rels.get("self");


        //update the submission
        //create a new submission
        HttpResponse<JsonNode> submissionPatchResponse = Unirest.patch(submissionStatusLocation)
                .headers(standardPostHeaders())
                .body("{\"status\": \"Submitted\"}")
                .asJson();


        assertThat(submissionPatchResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));
    }

    @Test
    public void submissionWithSamples() throws IOException, UnirestException {
        Map<String, String> rootRels = rootRels();

        String submissionLocation = submissionWithSamples(rootRels);


    }

    private String submissionWithSamples(Map<String, String> rootRels) throws UnirestException, IOException {
        Submission submission = Helpers.generateSubmission();
        HttpResponse<JsonNode> submissionResponse = postSubmission(rootRels, submission);

        String submissionLocation = submissionResponse.getHeaders().get("Location").get(0).toString();
        Map<String, String> submissionRels = relsFromPayload(submissionResponse.getBody().getObject());

        assertThat(submissionRels.get("samples"), notNullValue());

        List<Sample> testSamples = Helpers.generateTestSamples();
        //add samples to the submission
        for (Sample sample : testSamples) {

            sample.setSubmission(submissionLocation);

            HttpResponse<JsonNode> sampleResponse = Unirest.post(rootRels.get("samples"))
                    .headers(standardPostHeaders())
                    .body(sample)
                    .asJson();

            assertThat(sampleResponse.getStatus(), is(equalTo(HttpStatus.CREATED.value())));
        }

        //retrieve the samples
        String submissionSamplesUrl = submissionRels.get("samples");

        HttpResponse<JsonNode> samplesQueryResponse = Unirest.get(submissionSamplesUrl)
                .headers(standardGetHeaders())
                .asJson();

        assertThat(samplesQueryResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));

        JSONObject payload = samplesQueryResponse.getBody().getObject();
        JSONArray sampleList = payload.getJSONObject("_embedded").getJSONArray("samples");

        assertThat(sampleList.length(), is(equalTo(testSamples.size())));
        return submissionLocation;
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
        Map<String, String> rootRels = rootRels();


        int numberOfSubmissions = 5;

        Submission submission = Helpers.generateSubmission();
        List<Sample> testSamples = Helpers.generateTestSamples();

        for (int i = 0; i < numberOfSubmissions; i++) {
            HttpResponse<JsonNode> submissionResponse = postSubmission(rootRels, submission);

            String submissionLocation = submissionResponse.getHeaders().get("Location").get(0).toString();
            Map<String, String> submissionRels = relsFromPayload(submissionResponse.getBody().getObject());

            assertThat(submissionRels.get("samples"), notNullValue());

            //add samples to the submission
            for (Sample sample : testSamples) {

                sample.setSubmission(submissionLocation);

                HttpResponse<JsonNode> sampleResponse = Unirest.post(rootRels.get("samples"))
                        .headers(standardPostHeaders())
                        .body(sample)
                        .asJson();

                assertThat(sampleResponse.getStatus(), is(equalTo(HttpStatus.CREATED.value())));
            }
        }

        String domainName = submission.getDomain().getName();

        for (Sample sample : testSamples) {

            String sampleVersionsUrl = this.rootUri + "/domains/" + domainName + "/samples/" + sample.getAlias() + "/history"; //TODO this is bad, traverse rels!

            HttpResponse<JsonNode> response = Unirest.get(sampleVersionsUrl).headers(standardGetHeaders()).asJson();

            assertThat(response.getStatus(), is(equalTo(HttpStatus.OK.value())));

            JSONObject payload = response.getBody().getObject();

            JSONObject page = payload.getJSONObject("page");
            assertThat(page, notNullValue());
            assertThat(page.getInt("totalElements"), is(equalTo(numberOfSubmissions)));

            JSONArray jsonSamples = payload.getJSONObject("_embedded").getJSONArray("samples");
            assertThat(jsonSamples, notNullValue());
            assertThat(jsonSamples.length(), is(equalTo(numberOfSubmissions)));

            for (int i = 0; i < jsonSamples.length(); i++) {
                JSONObject jsonSample = jsonSamples.getJSONObject(i);

                String alias = jsonSample.getString("alias");
                assertThat(alias, is(sample.getAlias()));
            }
        }


    }


    private HttpResponse<JsonNode> postSubmission(Map<String, String> rootRels, Submission submission) throws UnirestException {
        //create a new submission
        HttpResponse<JsonNode> submissionResponse = Unirest.post(rootRels.get("submissions"))
                .headers(standardPostHeaders())
                .body(submission)
                .asJson();

        assertThat(submissionResponse.getStatus(), is(equalTo(HttpStatus.CREATED.value())));
        assertThat(submissionResponse.getHeaders().get("Location"), notNullValue());
        return submissionResponse;
    }

    @Test
    public void testPut() throws IOException, UnirestException {
        Map<String, String> rootRels = rootRels();

        Submission submission = Helpers.generateSubmission();
        HttpResponse<JsonNode> submissionResponse = postSubmission(rootRels, submission);

        String submissionLocation = submissionResponse.getHeaders().getFirst("Location");
        Map<String, String> submissionRels = relsFromPayload(submissionResponse.getBody().getObject());

        assertThat(submissionRels.get("samples"), notNullValue());

        Sample sample = Helpers.generateTestSamples().get(0);
        //add samples to the submission

        sample.setSubmission(submissionLocation);

        HttpResponse<JsonNode> sampleResponse = Unirest.post(rootRels.get("samples"))
                .headers(standardPostHeaders())
                .body(sample)
                .asJson();

        assertThat(sampleResponse.getStatus(), is(equalTo(HttpStatus.CREATED.value())));
        assertThat(sampleResponse.getHeaders().getFirst("Location"), notNullValue());

        String sampleLocation = sampleResponse.getHeaders().getFirst("Location");

        sample.setAlias("bob"); //modify the sample
        sample.setSubmission(submissionLocation);

        HttpResponse<JsonNode> samplePutResponse = Unirest.put(sampleLocation)
                .headers(standardPostHeaders())
                .body(sample)
                .asJson();

        logger.info("samplePutResponse: {}", samplePutResponse.getBody());
        assertThat(samplePutResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));


    }


    private Map<String, String> standardGetHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("accept", MediaTypes.HAL_JSON_VALUE);
        return h;
    }

    private Map<String, String> standardPostHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("accept", MediaTypes.HAL_JSON_VALUE);
        h.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        return h;
    }


    private Map<String, String> rootRels() throws UnirestException, IOException {
        HttpResponse<JsonNode> response = Unirest.get(rootUri)
                .headers(standardGetHeaders())
                .asJson();

        assertThat(response.getStatus(), is(equalTo(HttpStatus.OK.value())));
        JSONObject payload = response.getBody().getObject();

        return relsFromPayload(payload);
    }

    private Map<String, String> relsFromPayload(JSONObject payload) throws IOException {
        assertThat((Set<String>) payload.keySet(), hasItem("_links"));

        JSONObject links = payload.getJSONObject("_links");

        Map<String, String> rels = new HashMap<>();


        for (Object key : links.keySet()) {

            assertThat(key.getClass(), typeCompatibleWith(String.class));

            Object linkJson = links.get(key.toString());
            Link link = objectMapper.readValue(linkJson.toString(), Link.class);
            String href = link.withSelfRel().expand().getHref();

            rels.put((String) key, href);

        }
        return rels;
    }


}
