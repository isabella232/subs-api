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
import uk.ac.ebi.subs.data.client.Sample;
import uk.ac.ebi.subs.repository.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.SampleRepository;

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
    private SampleRepository sampleRepository;


    @Before
    public void buildUp() throws URISyntaxException {
        rootUri = "http://localhost:" + port + "/api";
        submissionRepository.deleteAll();
        sampleRepository.deleteAll();

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
    }

    @Test
    public void checkRootRels() throws UnirestException, IOException {
        Map<String, String> rootRels = rootRels();

        assertThat(rootRels.keySet(), hasItems("submissions", "samples"));
    }

    @Test
    public void simpleSubmissionWorkflow() throws IOException, UnirestException {
        Map<String, String> rootRels = rootRels();

        HttpResponse<JsonNode> submissionResponse = postSubmission(rootRels);

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

        //update the submission
        //create a new submission
        HttpResponse<JsonNode> submissionPatchResponse = Unirest.patch(submissionLocation)
                .headers(standardPostHeaders())
                .body("{\"status\": \"Submitted\"}")
                .asJson();


        assertThat(submissionPatchResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));
    }

    private HttpResponse<JsonNode> postSubmission(Map<String, String> rootRels) throws UnirestException {
        //create a new submission
        HttpResponse<JsonNode> submissionResponse = Unirest.post(rootRels.get("submissions"))
                .headers(standardPostHeaders())
                .body(Helpers.generateSubmission())
                .asJson();

        assertThat(submissionResponse.getStatus(), is(equalTo(HttpStatus.CREATED.value())));
        assertThat(submissionResponse.getHeaders().get("Location"), notNullValue());
        return submissionResponse;
    }

    @Test
    public void testPut() throws IOException, UnirestException {
        Map<String, String> rootRels = rootRels();

        HttpResponse<JsonNode> submissionResponse = postSubmission(rootRels);

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
        sample.setStatus("Draft"); // TODO move status out of the submittable so this is not necessary
        sample.setSubmission(submissionLocation);

        HttpResponse<JsonNode> samplePutResponse = Unirest.put(sampleLocation)
                .headers(standardPostHeaders())
                .body(sample)
                .asJson();

        logger.info("samplePutResponse: {}",samplePutResponse.getBody());
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
