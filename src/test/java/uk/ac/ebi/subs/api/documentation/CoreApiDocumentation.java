package uk.ac.ebi.subs.api.documentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.operation.preprocess.ContentModifier;
import org.springframework.restdocs.operation.preprocess.ContentModifyingOperationPreprocessor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.DocumentationProducer;
import uk.ac.ebi.subs.api.ApiIntegrationTestHelper;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.data.component.Attribute;
import uk.ac.ebi.subs.data.component.Term;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionPlanRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileRepositoryRest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.addAuthTokenHeader;

/**
 * Creates documentation snippets for core API features / behaviour
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@Category(DocumentationProducer.class)
@WithMockUser(username = "api_docs_usi_user", roles = {Helpers.TEAM_NAME})
public class CoreApiDocumentation {

    @Rule
    public final JUnitRestDocumentation restDocumentation = DocumentationHelper.jUnitRestDocumentation();

    @Value("${usi.docs.hostname:localhost}")
    private String host;
    @Value("${usi.docs.port:8080}")
    private int port;
    @Value("${usi.docs.scheme:http}")
    private String scheme;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionPlanRepository submissionPlanRepository;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private ProcessingStatusRepository processingStatusRepository;

    @Autowired
    private SampleRepository sampleRepository;

    @MockBean
    private DomainService domainService;
    @MockBean
    private ProfileRepositoryRest profileRepositoryRest;


    @Before
    public void setUp() {
        clearDbs();

        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);
        this.objectMapper = DocumentationHelper.mapper();

        ApiIntegrationTestHelper.mockAapProfileAndDomain(domainService,profileRepositoryRest);
        ApiIntegrationTestHelper.initialiseDataTypes(dataTypeRepository);
    }

    @After
    public void clearDbs(){
        Stream.of(
                submissionStatusRepository,
                submissionRepository,
                submissionPlanRepository,
                dataTypeRepository,
                processingStatusRepository,
                sampleRepository
        ).forEach(CrudRepository::deleteAll);
    }

    @Test
    public void root_endpoint_lists_links() throws Exception {

        this.mockMvc.perform(
                get("/api/")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("root-endpoint",
                                preprocessRequest(prettyPrint(), addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(

                                        halLinks(),
                                        //team
                                        linkWithRel("userTeams").description("Collection resource for teams"),
                                        linkWithRel("team").description("Templated link for finding one team"),
                                        //ref data
                                        linkWithRel("dataTypes").description("Collection resource for data types"),
                                        linkWithRel("submissionStatusDescriptions").description("Collection resource for submission status descriptions"),
                                        linkWithRel("processingStatusDescriptions").description("Collection resource for processing status descriptions "),
                                        linkWithRel("releaseStatusDescriptions").description("Collection resource for release status descriptions"),
                                        linkWithRel("uiSupportItems").description("Collection of data to populate help and example text in the UI"),
                                        linkWithRel("uiSupportItems:search").description("Search resource for UI support items"),
                                        linkWithRel("checklists").description("Collection of data checklists"),
                                        linkWithRel("checklists:search").description("Search resource for checklists"),
                                        linkWithRel("submissionPlans").description("Collection of submission plans, describing sets of data types that make sense to be submitted together"),
                                        //user projects
                                        linkWithRel("userProjects").description("Query resource for projects usable by the logged in user"),
                                        linkWithRel("userSubmissions").description("Query resource for submissions usable by the logged in user"),
                                        linkWithRel("userSubmissionStatusSummary").description("Query resource for counts of submission statuses for logged in user"),
                                        //profile
                                        linkWithRel("profile").description("Application level details"),
                                        //related services
                                        linkWithRel("aap-api-root").description("Link to the authentication authorisation and profile API"),
                                        linkWithRel("tus-upload").description("Link to the upload server, using the tus.io protocol")
                                ),
                                responseFields(
                                        DocumentationHelper.linksResponseField()
                                )
                        )
                );
    }

    @Test
    public void malformed_json_returns_bad_request() throws Exception {
        this.mockMvc.perform(
                post("/api/submissions").content("Tyger Tyger, burning bright, In the forests of the night")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isBadRequest())
                .andDo(
                        document("invalid-json",
                                preprocessRequest(prettyPrint(), addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("type").description("An URL to a document describing the error condition."),
                                        fieldWithPath("title").description("A brief title for the error condition."),
                                        fieldWithPath("status").description("The HTTP status code for the current request."),
                                        fieldWithPath("instance").description("URI identifying the specific instance of this problem."),
                                        fieldWithPath("errors").description("List of errors for this request.")
                                )
                        )
                );

    }

    @Test
    public void missing_information_in_post_returns_bad_request() throws Exception {
        uk.ac.ebi.subs.data.Submission submission = badClientSubmission();

        String jsonRepresentation = objectMapper.writeValueAsString(submission);


        this.mockMvc.perform(
                post("/api/submissions").content(jsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isBadRequest())
                .andDo(
                        document("invalid-submission",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("type").description("An URL to a document describing the error condition."),
                                        fieldWithPath("title").description("A brief title for the error condition."),
                                        fieldWithPath("status").description("The HTTP status code for the current request."),
                                        fieldWithPath("instance").description("URI identifying the specific instance of this problem."),
                                        fieldWithPath("errors").description("List of errors for this request.")
                                )
                        )
                );

    }

    private uk.ac.ebi.subs.data.Submission badClientSubmission() {
        return new uk.ac.ebi.subs.data.Submission();
    }

    @Test
    public void giving_json_array_instead_of_object_gives_bad_request() throws Exception {
        uk.ac.ebi.subs.data.Submission submission = SubmissionApiDocumentation.goodClientSubmission();

        String jsonRepresentation = objectMapper.writeValueAsString(Arrays.asList(submission, submission));

        this.mockMvc.perform(
                post("/api/submissions").content(jsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isBadRequest())
                .andDo(
                        document("json-array-instead-of-object",
                                preprocessRequest(prettyPrint(), addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("type").description("An URL to a document describing the error condition."),
                                        fieldWithPath("title").description("A brief title for the error condition."),
                                        fieldWithPath("status").description("The HTTP status code for the current request."),
                                        fieldWithPath("instance").description("URI identifying the specific instance of this problem."),
                                        fieldWithPath("errors").description("List of errors for this request.")
                                )
                        )
                );

    }

    @Test
    public void example_of_conditional_request() throws Exception {
        Submission sub = storeSubmission();
        List<Sample> samples = storeSamples(sub, 1);
        Sample s = samples.get(0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM YYYY H:m:s zzz");

        String etagValueString = String.format("ETag: \"%d\"", s.getVersion());
        String lastModifiedString = dateFormat.format(s.getLastModifiedDate());

        this.mockMvc.perform(
                get("/api/samples/{sampleId}", s.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-None-Match", etagValueString)
        ).andExpect(status().isNotModified())
                .andDo(
                        document("conditional-fetch-etag-get-if-none-match",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()
                                )
                        )
                );

        this.mockMvc.perform(
                delete("/api/samples/{sampleId}", s.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-Match", "ETag: \"10\"")
        ).andExpect(status().isPreconditionFailed())
                .andDo(
                        document("conditional-delete-if-etag-match",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()
                                )
                        )
                );

        this.mockMvc.perform(
                get("/api/samples/{sampleId}", s.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-Modified-Since", lastModifiedString)
        ).andExpect(status().isNotModified())
                .andDo(
                        document("conditional-fetch-if-modified-since",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()
                                )
                        )
                );
    }

    private List<Sample> storeSamples(Submission sub, int numberRequired) {
        List<Sample> samples = Helpers.generateTestSamples(numberRequired);

        for (Sample s : samples) {
            s.setCreatedDate(new Date());
            s.setSubmission(sub);

            Attribute cellLineType = Helpers.attribute("EBV-LCL cell line");
            Term ebvLclCellLine = new Term();
            ebvLclCellLine.setUrl("http://purl.obolibrary.org/obo/BTO_0003335");
            cellLineType.getTerms().add(ebvLclCellLine);

            s.getAttributes().put("Cell line type", Collections.singletonList(cellLineType));

            processingStatusRepository.insert(s.getProcessingStatus());
            sampleRepository.insert(s);
        }
        return samples;
    }

    @Test
    public void example_404_error() throws Exception {
        this.mockMvc.perform(get("/api/samples/123456789"))
                .andExpect(status().isNotFound())
                .andDo(
                        document("sample-not-found",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint())
                        )
                );
    }

    @Test
    public void example_methodNotAllowed_error() throws Exception {
        this.mockMvc.perform(get("/api/submissions"))
                .andExpect(status().isMethodNotAllowed())
                .andDo(
                        document("method-not-allowed",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint())
                        )
                );
    }

    @Test
    public void example_of_paging_through_a_list() throws Exception {

        String teamName = null;
        for (int i = 0; i < 50; i++) {
            Submission s = Helpers.generateTestSubmission();
            submissionStatusRepository.insert(s.getSubmissionStatus());
            submissionRepository.insert(s);
            teamName = s.getTeam().getName();
        }

        this.mockMvc.perform(get("/api/submissions/search/by-team?teamName={teamName}&page=1&size=10", teamName))
                .andExpect(status().isOk())
                .andDo(document(
                        "page-example",
                        preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                        preprocessResponse(maskEmbedded(), prettyPrint()),

                        links(halLinks(),
                                linkWithRel("self").description("This resource list"),
                                linkWithRel("first").description("The first page in the resource list"),
                                linkWithRel("next").description("The next page in the resource list"),
                                linkWithRel("prev").description("The previous page in the resource list"),
                                linkWithRel("last").description("The last page in the resource list")
                        ),
                        responseFields(
                                fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources"),
                                fieldWithPath("_embedded").description("The list of resources"),
                                fieldWithPath("page.size").description("The number of resources in this page"),
                                fieldWithPath("page.totalElements").description("The total number of resources"),
                                fieldWithPath("page.totalPages").description("The total number of pages"),
                                fieldWithPath("page.number").description("The page number")
                        )
                ));
    }

    public ContentModifyingOperationPreprocessor maskEmbedded() {
        return new ContentModifyingOperationPreprocessor(new MaskElement("_embedded"));
    }

    public ContentModifyingOperationPreprocessor maskLinks() {
        return new ContentModifyingOperationPreprocessor(new MaskElement("_links"));
    }

    protected class MaskElement implements ContentModifier {

        private String keyToRemove;

        public MaskElement(String keyToRemove) {
            this.keyToRemove = keyToRemove;
        }

        @Override
        public byte[] modifyContent(byte[] originalContent, MediaType contentType) {
            TypeReference<HashMap<String, Object>> typeRef
                    = new TypeReference<HashMap<String, Object>>() {
            };

            Map<String, Object> o = null;
            try {
                o = objectMapper.readValue(originalContent, typeRef);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            o.put("_embedded", "...");
            try {
                return objectMapper.writeValueAsBytes(o);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Submission storeSubmission() {
        Submission sub = Helpers.generateTestSubmission();

        this.submissionStatusRepository.insert(sub.getSubmissionStatus());
        this.submissionRepository.save(sub);
        this.submissionPlanRepository.save(sub.getSubmissionPlan());

        return sub;
    }
}
