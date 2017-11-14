package uk.ac.ebi.subs.api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.data.client.Sample;
import uk.ac.ebi.subs.repository.model.Submission;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("aap")
public class AAPIntegrationTest extends ApiIntegrationTest {

    @Value("${aap.url}")
    private String aapURL;

    @Value("${aap.username}")
    private String aapUsername;

    @Value("${aap.password}")
    private String appPassword;

    @Override
    public Map<String, String> createGetHeaders() throws UnirestException {
        final Map<String, String> getHeaders = super.createGetHeaders();
        String jwtToken = getJWTToken(aapURL, aapUsername, appPassword);
        getHeaders.put("Authorization", "Bearer " + jwtToken);
        return getHeaders;
    }

    @Override
    public Map<String, String> createPostHeaders() throws UnirestException {
        final Map<String, String> postHeaders = super.createPostHeaders();
        String jwtToken = getJWTToken(aapURL, aapUsername, appPassword);
        postHeaders.put("Authorization", "Bearer " + jwtToken);
        return postHeaders;
    }

    String getJWTToken(String authURL, String username, String password) throws UnirestException {
        final HttpResponse<String> stringHttpResponse = Unirest.get(authURL).basicAuth(username, password).asString();
        return stringHttpResponse.getBody();
    }

    @Test
    public void checkHealthPage() throws IOException, UnirestException {
        String uri = rootUri + "/health";


        //no headers set, we want this to work without a token
        HttpResponse<JsonNode> healthStatusResponse =
                Unirest.get(uri)
                        .asJson();

        //test environment likely to return 'down', key point is that it isn't a 401 or 403
        assertThat(healthStatusResponse.getStatus(), isIn(
                Arrays.asList(
                        HttpStatus.OK.value(),
                        HttpStatus.SERVICE_UNAVAILABLE.value()
                )
        ));
    }

    @Test
    public void postSubmission() throws UnirestException, IOException {
        Map<String, String> rootRels = testHelper.rootRels();

        Submission submission = new Submission();
        HttpResponse<JsonNode> submissionResponse = testHelper.postSubmission(rootRels, submission);

        JSONObject submissionResponseObject = submissionResponse.getBody().getObject();
        JSONObject submitterObject = submissionResponseObject.getJSONObject("submitter");
        assertThat(submitterObject.get("email"), notNullValue());
        assertThat(submitterObject.get("email"), is(equalTo("subs-internal@ebi.ac.uk")));
    }

    @Test
    public void emulateCorsPreflight() throws IOException, UnirestException {
        Map<String, String> rootRels = testHelper.rootRels();

        //OPTIONS calls should not require authentication

        String teamsUrl = rootRels.get("userTeams");
        HttpResponse<String> optionsResponse = Unirest.options(teamsUrl)
                .header("Origin", "http://evil.com")
                .header("Access-Control-Request-Method", "PUT")
                .asString();

        assertThat(optionsResponse.getStatus(), equalTo(HttpStatus.OK.value()));
    }

    @After
    public void tearDown() throws IOException {
        submissionRepository.deleteAll();
        sampleRepository.deleteAll();
        submissionStatusRepository.deleteAll();
    }
}
