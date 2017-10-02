package uk.ac.ebi.subs.api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.ApiApplication;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.Matchers.isIn;
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
        String jwtToken = getJWTToken(aapURL,aapUsername,appPassword);
        getHeaders.put("Authorization", "Bearer " + jwtToken);
        return getHeaders;
    }

    @Override
    public Map<String, String> createPostHeaders() throws UnirestException {
        final Map<String, String> postHeaders = super.createPostHeaders();
        String jwtToken = getJWTToken(aapURL,aapUsername,appPassword);
        postHeaders.put("Authorization", "Bearer " + jwtToken);
        return postHeaders;
    }

    String getJWTToken (String authURL, String username, String password) throws UnirestException {
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

}
