package uk.ac.ebi.subs.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.utils.Base64Coder;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.client.Sample;
import uk.ac.ebi.subs.data.client.Study;
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.Profile;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileRepositoryRest;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.typeCompatibleWith;
import static org.junit.Assert.assertThat;

/**
 * Created by davidr on 24/02/2017.
 */
public class ApiIntegrationTestHelper {

    private ObjectMapper objectMapper;
    private String rootUri;

    private Map<String, String> getHeaders = new HashMap<>();
    private Map<String, String> postHeaders = new HashMap<>();

    public ApiIntegrationTestHelper(ObjectMapper objectMapper, String rootUri, List<MongoRepository> repositoriesToInit,
                                    Map<String, String> getHeaders, Map<String, String> postHeaders) {
        this.objectMapper = objectMapper;
        this.rootUri = rootUri;
        this.getHeaders = getHeaders;
        this.postHeaders = postHeaders;

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

        repositoriesToInit.forEach(MongoRepository::deleteAll);
    }

    public HttpResponse<JsonNode> postSubmission(Map<String, String> rootRels, Submission submission) throws UnirestException, IOException {
        Map<String, String> teamRels = teamRels(Helpers.TEAM_NAME);
        //create a new submission
        HttpResponse<JsonNode> submissionResponse = Unirest.post(teamRels.get("submissions:create"))
                .headers(postHeaders)
                .body(submission)
                .asJson();

        assertThat(submissionResponse.getStatus(), is(equalTo(HttpStatus.CREATED.value())));
        assertThat(submissionResponse.getHeaders().get("Location"), notNullValue());
        return submissionResponse;
    }

    public String submissionWithSamples(Submission submission, Map<String, String> rootRels) throws UnirestException, IOException {
        Map<String,String> teamRels = teamRels(Helpers.TEAM_NAME);
        HttpResponse<JsonNode> submissionResponse = postSubmission(teamRels, submission);

        String submissionLocation = submissionResponse.getHeaders().getFirst("Location");
        Map<String, String> submissionRels = relsFromPayload(submissionResponse.getBody().getObject());
        Map<String,String> submissionContentsRels = relsFromUri(submissionRels.get("contents"));

        assertThat(submissionContentsRels.get("samples:create"), notNullValue());

        List<Sample> testSamples = Helpers.generateTestClientSamples(2);
        //add samples to the submission
        for (Sample sample : testSamples) {

            HttpResponse<JsonNode> sampleResponse = Unirest.post(submissionContentsRels.get("samples:create"))
                    .headers(postHeaders)
                    .body(sample)
                    .asJson();

            assertThat(sampleResponse.getStatus(), is(equalTo(HttpStatus.CREATED.value())));

            JsonNode sampleResourceBody = sampleResponse.getBody();
            JSONObject embeddedResource = sampleResourceBody.getObject().getJSONObject("_embedded");
            assertThat(embeddedResource,notNullValue());
            assertThat(embeddedResource.getJSONObject("processingStatus"),notNullValue());
            assertThat(embeddedResource.getJSONObject("submission"),notNullValue());
            assertThat(embeddedResource.getJSONObject("validationResult"),notNullValue());
        }

        // TODO - look up if this is needed and why is it getting a 500 Apache Error
        //retrieve the samples
        /*
        String submissionSamplesUrl = submissionContentsRels.get("samples");

        HttpResponse<JsonNode> samplesQueryResponse = Unirest.get(submissionSamplesUrl)
                .headers(getHeaders)
                .asJson();

        assertThat(samplesQueryResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));

        JSONObject payload = samplesQueryResponse.getBody().getObject();
        JSONArray sampleList = payload.getJSONObject("_embedded").getJSONArray("samples");

        assertThat(sampleList.length(), is(equalTo(testSamples.size())));
        */
        return submissionLocation;
    }

    public String submissionWithStudies(Map<String, String> rootRels) throws UnirestException, IOException {
        Submission submission = Helpers.generateSubmission();
        HttpResponse<JsonNode> submissionResponse = postSubmission(rootRels, submission);

        String submissionLocation = submissionResponse.getHeaders().getFirst("Location");
        Map<String, String> submissionRels = relsFromPayload(submissionResponse.getBody().getObject());
        Map<String,String> submissionContentsRels = relsFromUri(submissionRels.get("contents"));
        assertThat(submissionContentsRels.get("sequencingStudies:create"), notNullValue());

        List<Study> testStudies = Helpers.generateTestClientStudies(2);
        //add samples to the submission
        for (Study study : testStudies) {

            HttpResponse<JsonNode> studyResponse = Unirest.post(submissionContentsRels.get("sequencingStudies:create"))
                    .headers(postHeaders)
                    .body(study)
                    .asJson();

            assertThat(studyResponse.getStatus(), is(equalTo(HttpStatus.CREATED.value())));
        }

        // TODO - look up if this is needed and why is it getting a 500 Apache Error
        //retrieve the studies
        /*
        String submissionStudiesUrl = submissionContentsRels.get("studies");

        HttpResponse<JsonNode> studiesQueryResponse = Unirest.get(submissionStudiesUrl)
                .headers(getHeaders)
                .asJson();

        assertThat(studiesQueryResponse.getStatus(), is(equalTo(HttpStatus.OK.value())));

        JSONObject payload = studiesQueryResponse.getBody().getObject();
        JSONArray studyList = payload.getJSONObject("_embedded").getJSONArray("studies");

        assertThat(studyList.length(), is(equalTo(testStudies.size())));
        */
        return submissionLocation;
    }

    public Map<String, String> rootRels() throws UnirestException, IOException {
       return relsFromUri(rootUri);
    }

    public Map<String, String> relsFromUri(String url) throws UnirestException, IOException{
        HttpResponse<JsonNode> response = Unirest.get(url)
                .headers(getHeaders)
                .asJson();

        assertThat(response.getStatus(), is(equalTo(HttpStatus.OK.value())));
        JSONObject payload = response.getBody().getObject();

        return relsFromPayload(payload);
    }

    public Map<String, String> teamRels(String teamName) throws UnirestException, IOException {
        Assert.notNull(teamName);
        Map<String,String> rootRels = rootRels();
        String teamRel = rootRels.get("team");
        String teamUri = teamRel.replace("{teamName:.+}",teamName);

        return relsFromUri(teamUri);
    }

    public Map<String, String> relsFromPayload(JSONObject payload) throws IOException {
        assertThat((Set<String>) payload.keySet(), hasItem("_links"));

        JSONObject links = payload.getJSONObject("_links");

        Map<String, String> rels = new HashMap<>();


        for (Object key : links.keySet()) {

            assertThat(key.getClass(), typeCompatibleWith(String.class));

            Object linkJson = links.get(key.toString());
            Link link = objectMapper.readValue(linkJson.toString(), Link.class);

            rels.put((String) key, link.getHref());

        }
        return rels;
    }


    public static Map<String, String> createBasicAuthheaders (String userName, String password) {
        Map<String, String> h = new HashMap<>();
        h.put(HttpHeaders.AUTHORIZATION, "Basic " + Base64Coder.encodeString(userName + ":" + password));
        return h;
    }

    public static Map<String, String> createStandardGetHeader() {
        Map<String, String> h = new HashMap<>();
        h.put(HttpHeaders.ACCEPT, MediaTypes.HAL_JSON_VALUE);
        h.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return h;
    }

    public static Map<String, String> createStandardPostHeader() {
        Map<String, String> h = new HashMap<>();
        h.putAll(createStandardGetHeader());
        h.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return h;
    }

    public Map<String, String> getGetHeaders() {
        return getHeaders;
    }

    public Map<String, String> getPostHeaders() {
        return postHeaders;
    }

    public static void mockAapProfileAndDomain(DomainService domainService, ProfileRepositoryRest profileRepositoryRest){
        Domain domain = new Domain();
        domain.setDomainReference("1234");
        domain.setDomainName(Helpers.TEAM_NAME);
        Map<String, String> attributes = new HashMap<>();
        attributes.put("centre name", "An Institute");
        Profile profile = new Profile(domain.getDomainReference(), null, domain, attributes);

        Mockito.when(domainService.getMyDomains(Mockito.anyString()))
                .thenReturn(Arrays.asList(
                        domain
                ));

        Mockito.when(profileRepositoryRest.getDomainProfile(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(profile);
    }

    public static List<DataType>  initialiseDataTypes(DataTypeRepository dataTypeRepository){
        List<DataType> dataTypes = Arrays.asList(
                buildDataType("samples", uk.ac.ebi.subs.repository.model.Sample.class, "sample", "samples", Archive.BioSamples),
                buildDataType("projects", uk.ac.ebi.subs.repository.model.Project.class, "project", "projects", Archive.BioStudies),
                buildDataType("enaStudies", uk.ac.ebi.subs.repository.model.Study.class, "sequencing studies", "sequencing study", Archive.Ena),
                buildDataType("sequencingExperiments", uk.ac.ebi.subs.repository.model.Assay.class ,"sequencing experiment", "sequencing assays", Archive.Ena),
                buildDataType("sequencingRuns", uk.ac.ebi.subs.repository.model.AssayData.class, "sequencing runs", "sequencing run", Archive.Ena)
        );
        dataTypeRepository.insert(dataTypes);
        return dataTypes;
    }

    private static DataType buildDataType(String id, Class clazz, String singularName, String pluralName, Archive archive){
        DataType dt = new DataType();
        dt.setId(id);
        dt.setSubmittableClassName(clazz.getName());
        dt.setArchive(archive);
        dt.setDescription("<<data type description>>");
        dt.setDisplayNamePlural(pluralName);
        dt.setDisplayNameSingular(singularName);

        Map<String,String> schemaMap = new HashMap<>();
        schemaMap.put("$schema","http://json-schema.org/draft-07/schema#");
        schemaMap.put("description","<<JSON schema used to validate this data type>>");
        ObjectMapper om = new ObjectMapper();
        dt.setValidationSchema(om.valueToTree(schemaMap));
        return dt;
    }

}
