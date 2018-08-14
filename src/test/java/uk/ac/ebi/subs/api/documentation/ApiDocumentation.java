package uk.ac.ebi.subs.api.documentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.hypermedia.LinkDescriptor;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.operation.preprocess.ContentModifier;
import org.springframework.restdocs.operation.preprocess.ContentModifyingOperationPreprocessor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.DocumentationProducer;
import uk.ac.ebi.subs.api.ApiIntegrationTestHelper;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.api.services.SubmissionEventService;
import uk.ac.ebi.subs.api.services.SubmissionStatusService;
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.component.Attribute;
import uk.ac.ebi.subs.data.component.SampleRelationship;
import uk.ac.ebi.subs.data.component.Term;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.repository.model.Assay;
import uk.ac.ebi.subs.repository.model.AssayData;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Study;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.AssayDataRepository;
import uk.ac.ebi.subs.repository.repos.submittables.AssayRepository;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;
import uk.ac.ebi.subs.repository.repos.submittables.StudyRepository;
import uk.ac.ebi.subs.validator.data.SingleValidationResult;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.data.structures.GlobalValidationStatus;
import uk.ac.ebi.subs.validator.data.structures.SingleValidationResultStatus;
import uk.ac.ebi.subs.validator.data.structures.ValidationAuthor;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;
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
import java.util.stream.IntStream;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.addAuthTokenHeader;

/**
 * Use this class to create document snippets. Ascii docotor will weave them into html documents,
 * using the files in src/resources/docs/ascidocs
 *
 * @see <a href="https://github.com/EBISPOT/OLS/blob/master/ols-web/src/test/java/uk/ac/ebi/spot/ols/api/ApiDocumentation.java">OLS ApiDocumentation.java</a>
 * <p>
 * gives this
 * @see <a href="http://www.ebi.ac.uk/ols/docs/api">OLS API Docs<</a>
 * <p>
 * API documentation should learn from the excellent example at @see <a href="https://developer.github.com/v3/">GitHub</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@Category(DocumentationProducer.class)
@WithMockUser(username = "api_docs_usi_user", roles = {Helpers.TEAM_NAME})
public class ApiDocumentation {

    @Rule
    public final JUnitRestDocumentation restDocumentation = DocumentationHelper.jUnitRestDocumentation();

    @Value("${usi.docs.hostname:localhost}")
    private String host;
    @Value("${usi.docs.port:8080}")
    private int port;
    @Value("${usi.docs.scheme:http}")
    private String scheme;

    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;
    @Autowired
    private SampleRepository sampleRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProcessingStatusRepository processingStatusRepository;
    @Autowired
    private ValidationResultRepository validationResultRepository;
    @Autowired
    private AssayRepository assayRepository;
    @Autowired
    private AssayDataRepository assayDataRepository;
    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;
    @MockBean
    private DomainService domainService;
    @MockBean
    private ProfileRepositoryRest profileRepositoryRest;
    @MockBean
    private SubmissionStatusService submissionStatusService;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;
    private SubmissionEventService fakeSubmissionEventService = DocumentationHelper.fakeSubmissionEventService();

    @Before
    public void setUp() {
        storeSubmission();
        clearDatabases();
        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);
        this.objectMapper = DocumentationHelper.mapper();

        ApiIntegrationTestHelper.mockAapProfileAndDomain(domainService,profileRepositoryRest);

        Mockito.when(submissionStatusService.isSubmissionStatusChangeable(Mockito.any(Submission.class)))
                .thenReturn(Boolean.TRUE);
        Mockito.when(submissionStatusService.isSubmissionStatusChangeable(Mockito.any(SubmissionStatus.class)))
                .thenReturn(Boolean.TRUE);

    }

    @Test
    public void invalidJson() throws Exception {
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
    public void jsonArrayInsteadOfObject() throws Exception {
        uk.ac.ebi.subs.data.Submission submission = goodClientSubmission();

        String jsonRepresentation = objectMapper.writeValueAsString(Arrays.asList(submission, submission));

        this.mockMvc.perform(
                post("/api/submissions").content(jsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isBadRequest())
                .andDo(
                        document("json-array-instead-of-object",
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

    @Test
    public void sampleNotFound() throws Exception {
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
    public void methodNotAllowed() throws Exception {
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
    public void invalidSubmission() throws Exception {
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

    @Test
    public void validSubmission() throws Exception {
        uk.ac.ebi.subs.data.Submission submission = goodClientSubmission();

        String jsonRepresentation = objectMapper.writeValueAsString(submission);

        Mockito.when(submissionStatusService.getAvailableStatusNames(Mockito.any(Submission.class),Mockito.anyMap()))
                .thenReturn(Arrays.asList("Submitted"));

        this.mockMvc.perform(
                post("/api/teams/" + Helpers.TEAM_NAME + "/submissions").content(jsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)
                        .header(DocumentationHelper.AUTHORIZATION_HEADER_NAME,DocumentationHelper.AUTHORIZATION_HEADER_VALUE)

        ).andExpect(status().isCreated())
                .andDo(
                        document("create-submission",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("submitter").description("Submitter who is responsible for this submission"),
                                        fieldWithPath("team").description("Team this submission belongs to"),

                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("submission").description("This submission"),
                                        linkWithRel("self:update").description("This submission can be updated"),
                                        linkWithRel("self:delete").description("This submission can be deleted"),
                                        linkWithRel("team").description("The team this submission belongs to"),
                                        linkWithRel("contents").description("The contents of this submission"),
                                        linkWithRel("submissionStatus").description("The status of this submission"),
                                        linkWithRel("submissionStatus:update").description("The submission status can be changed"),
                                        linkWithRel("availableStatuses").description("List of values that you can set the submission status to"),
                                        linkWithRel("processingStatuses").description("All processing statuses for the contents of this submission"),
                                        linkWithRel("validationResults").description("All validation results for the contents of this submission"),
                                        linkWithRel("processingStatusSummary").description("Summary of processing statuses for this submission"),
                                        linkWithRel("typeProcessingStatusSummary").description("Summary of processing statuses per type, for this submission")


                                )
                        )
                );


        Submission sub = submissionRepository.findAll().get(0);

        this.mockMvc.perform(get("/api/submissions/{submissionId}/contents", sub.getId()))
                .andExpect(status().isOk())
                .andDo(document(
                        "submission-contents",
                        preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                        preprocessResponse(prettyPrint()),
                        links(
                                halLinks(),
                                linkWithRel("analyses").description("Collection of analyses within this submission"),
                                linkWithRel("analyses:create").description("Create a new analysis resource"),
                                linkWithRel("assayData").description("Collection of assay data within this submission"),
                                linkWithRel("assayData:create").description("Create a new assay data resource"),
                                linkWithRel("assays").description("Collection of assays within this submission"),
                                linkWithRel("assays:create").description("Create a new assay resource"),
                                linkWithRel("egaDacPolicies").description("Collection of DAC policies within this submission"),
                                linkWithRel("egaDacPolicies:create").description("Create a new DAC policy resource"),
                                linkWithRel("egaDacs").description("Collection of DACs within this submission"),
                                linkWithRel("egaDacs:create").description("Create a new DAC resource"),
                                linkWithRel("egaDatasets").description("Collection of EGA Datasets within this submission"),
                                linkWithRel("egaDatasets:create").description("Create a new EGA dataset resource"),
                                linkWithRel("files").description("Collection of files within this submission"),
                                linkWithRel("projects:create").description("Create a new project resource"),
                                linkWithRel("protocols").description("Collection of protocols within this submission"),
                                linkWithRel("protocols:create").description("Create a new protocol resource"),
                                linkWithRel("sampleGroups").description("Collection of sample groups within this submission"),
                                linkWithRel("sampleGroups:create").description("Create a new sample group resource"),
                                linkWithRel("samples").description("Collection of samples within this submission"),
                                linkWithRel("samples:create").description("Create a new sample resource"),
                                linkWithRel("sheetUpload").description("Upload a spreadsheet of submittables, based on a template"),
                                linkWithRel("studies").description("Collection of studies within this submission"),
                                linkWithRel("studies:create").description("Create a new study resource"),
                                linkWithRel("samplesSheets").description("Samples spreadsheets that have been uploaded but not processed")

                        ),
                        responseFields(
                                fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources")
                        )
                ));


        SingleValidationResult svr = new SingleValidationResult();
        svr.setValidationStatus(SingleValidationResultStatus.Pass);

        ValidationResult vr = new ValidationResult();
        vr.setSubmissionId(sub.getId());
        vr.setUuid("test");
        vr.getExpectedResults().put(ValidationAuthor.Core,Arrays.asList(svr));
        vr.setValidationStatus(GlobalValidationStatus.Complete);
        validationResultRepository.insert(vr);

        SubmissionStatus status = submissionStatusRepository.findAll().get(0);
        Assert.notNull(status);

        this.mockMvc.perform(get("/api/submissions/{submissionId}/availableSubmissionStatuses", sub.getId()))
                .andExpect(status().isOk())
                .andDo(document(
                        "available-status-report",
                        preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("_links").description("Links"),
                                fieldWithPath("_embedded").description("The list of resources")
                        ),
                        links(
                                halLinks(),
                                linkWithRel("self").description("This resource"),
                                linkWithRel("submission").description("This submission")
                        )
                ));

        this.mockMvc.perform(
                put("/api/submissions/{id}/submissionStatus", sub.getId()).content("{\"status\": \"Submitted\"}")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isOk())
                .andDo(
                        document("change-submission-status",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("status").description("Current status value"),
                                        fieldWithPath("team").description("Team that owns this submission"),
                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("self:update").description("This resource is updateable"),
                                        linkWithRel("submissionStatus").description("This resource"),
                                        linkWithRel("statusDescription").description("Description of this status"),
                                        linkWithRel("availableStatuses").description("List of status values that you can currently use in this resource")
                                )
                        )
                );

    }

    @Test
    public void progressReports() throws Exception {
        Submission sub = storeSubmission();

        fakeProcessingStatus(sub);

        this.mockMvc.perform(get("/api/processingStatuses/search/findBySubmissionId?submissionId={submissionId}", sub.getId()))
                .andExpect(status().isOk())
                .andDo(document(
                        "page-progress-reports",
                        preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                        preprocessResponse(prettyPrint()),
                        links(halLinks(),
                                linkWithRel("self").description("This resource list"),
                                linkWithRel("first").description("The first page in the resource list"),
                                linkWithRel("next").description("The next page in the resource list"),
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

        this.mockMvc.perform(get("/api/submissions/{submissionId}/processingStatusSummaryCounts", sub.getId()))
                .andExpect(status().isOk())
                .andDo(document(
                        "summary-progress-reports",
                        preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                        preprocessResponse(prettyPrint()),
                        links(halLinks()
                        ),
                        responseFields(
                                fieldWithPath("Dispatched").description("The number of documents dispatched to an archive"),
                                fieldWithPath("Completed").description("The number of documents that are completely archived")
                        )
                ));

        this.mockMvc.perform(get("/api/submissions/{submissionId}/processingStatusSummaryTypeCounts", sub.getId()))
                .andExpect(status().isOk())
                .andDo(document(
                        "type-summary-progress-reports",
                        preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                        preprocessResponse(prettyPrint()),
                        links(halLinks()
                        ),
                        responseFields(
                                fieldWithPath("Assay").description("Counts of statuses for this document type"),
                                fieldWithPath("AssayData").description("Counts of statuses for this document type"),
                                fieldWithPath("Study").description("Counts of statuses for this document type"),
                                fieldWithPath("Sample").description("Counts of statuses for this document type")
                        )
                ));

        this.mockMvc.perform(get("/api/submissions/{submissionId}/availableSubmissionStatuses", sub.getId()))
                .andExpect(status().isOk())
                .andDo(document(
                        "available-status-reports",
                        preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("_links").description("Links")
                        ),
                        links(
                                halLinks(),
                                linkWithRel("self").description("This resource"),
                                linkWithRel("submission").description("This submission")
                        )
                ));
    }

    @Test
    public void createStudy() throws Exception {
        Submission sub = storeSubmission();

        uk.ac.ebi.subs.data.client.Study study = Helpers.generateTestClientStudies(1).get(0);

        String jsonRepresentation = objectMapper.writeValueAsString(study);

        this.mockMvc.perform(
                post("/api/submissions/" + sub.getId() + "/contents/studies").content(jsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isCreated())
                .andDo(
                        document("create-study",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the study within the team"),
                                        fieldWithPath("title").description("Title for the study"),
                                        fieldWithPath("description").description("Description for the study"),
                                        fieldWithPath("attributes").description("A list of attributes for the study"),

                                        fieldWithPath("studyType").description("Type of data in this study"),
                                        fieldWithPath("protocolRefs").description("References to protocols used in this study"),
                                        fieldWithPath("projectRef").description("References to the overall project that this study is part of"),

                                        fieldWithPath("_embedded.submission").description("Submission that this study is part of"),
                                        fieldWithPath("_embedded.processingStatus").description("Processing status for this study."),
                                        fieldWithPath("_embedded.validationResult").description("Validation result for this study."),
                                        fieldWithPath("team").description("Team this sample belongs to"),
                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        validationresultLink(),
                                        submissionLink(),
                                        processingStatusLink(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("study").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("history").description("Collection of resources for samples with the same team and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by team and alias"),
                                        linkWithRel("dataType").description("Resource describing the requirements for this data type"),
                                        linkWithRel("checklist").description("Resource describing opt-in data requirements for this document")
                                )
                        )
                );
    }

    @Test
    public void createAssay() throws Exception {
        Submission sub = storeSubmission();
        uk.ac.ebi.subs.data.client.Assay assay = Helpers.generateTestClientAssays(1).get(0);

        String jsonRepresentation = objectMapper.writeValueAsString(assay);

        this.mockMvc.perform(
                post("/api/submissions/" + sub.getId() + "/contents/assays").content(jsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isCreated())
                .andDo(
                        document("create-assay",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the study within the team"),
                                        fieldWithPath("title").description("Title for the study"),
                                        fieldWithPath("description").description("Description for the study"),
                                        fieldWithPath("attributes").description("A list of attributes for the study"),


                                        fieldWithPath("studyRef").description("Reference to the study that this assay is part of"),
                                        fieldWithPath("sampleUses").description("Samples used in this assay"),
                                        fieldWithPath("sampleUses[0].sampleRef").description("Reference to the sample used in this assay"),
                                        fieldWithPath("protocolUses").description("Protocols used in this study"),

                                        fieldWithPath("_embedded.submission").description("Submission that this study is part of"),
                                        fieldWithPath("_embedded.processingStatus").description("Processing status for this assay."),
                                        fieldWithPath("_embedded.validationResult").description("Validation result for this assay."),
                                        fieldWithPath("team").description("Team this sample belongs to"),
                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        validationresultLink(),
                                        submissionLink(),
                                        processingStatusLink(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("assay").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("history").description("Collection of resources for samples with the same team and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by team and alias"),
                                        linkWithRel("dataType").description("Resource describing the requirements for this data type"),
                                        linkWithRel("checklist").description("Resource describing opt-in data requirements for this document")
                                )
                        )
                );
    }

    @Test
    public void createAssayData() throws Exception {
        Submission sub = storeSubmission();
        uk.ac.ebi.subs.data.client.AssayData assayData = Helpers.generateTestClientAssayData(1).get(0);

        String jsonRepresentation = objectMapper.writeValueAsString(assayData);

        this.mockMvc.perform(
                post("/api/submissions/" + sub.getId() + "/contents/assayData").content(jsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isCreated())
                .andDo(
                        document("create-assay-data",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the study within the team"),
                                        fieldWithPath("title").description("Title for the study"),
                                        fieldWithPath("description").description("Description for the study"),
                                        fieldWithPath("attributes").description("A list of attributes for the study"),

                                        fieldWithPath("assayRefs").description("Reference to the assays that this data is generated from"),

                                        fieldWithPath("files").description("Files used in this submission"),
                                        fieldWithPath("files[0].name").description("File name"),
                                        fieldWithPath("files[0].checksum").description("File checksum using md5"),
                                        fieldWithPath("files[0].type").description("File type"),

                                        fieldWithPath("_embedded.submission").description("Submission that this study is part of"),
                                        fieldWithPath("_embedded.processingStatus").description("Processing status for this assay data."),
                                        fieldWithPath("_embedded.validationResult").description("Validation result for this study."),
                                        fieldWithPath("team").description("Team this sample belongs to"),
                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        validationresultLink(),
                                        submissionLink(),
                                        processingStatusLink(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("assayData").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("history").description("Collection of resources for samples with the same team and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by team and alias"),
                                        linkWithRel("dataType").description("Resource describing the requirements for this data type"),
                                        linkWithRel("checklist").description("Resource describing opt-in data requirements for this document")
                                )
                        )
                );
    }

    @Test
    public void createProject() throws Exception {
        Submission sub = storeSubmission();
        uk.ac.ebi.subs.data.client.Project project = Helpers.generateClientProject();

        String jsonRepresentation = objectMapper.writeValueAsString(project);

        this.mockMvc.perform(
                post("/api/submissions/" + sub.getId() + "/contents/projects/").content(jsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isCreated())
                .andDo(
                        document("create-project",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the project within the team"),
                                        fieldWithPath("title").description("Title for the project"),
                                        fieldWithPath("description").description("Description for the project"),
                                        fieldWithPath("contacts").description("Contacts for this project"),
                                        fieldWithPath("publications").description("Publications related to thisproject"),
                                        //fieldWithPath("attributes").description("A list of attributes for the project"),
                                        fieldWithPath("_embedded.submission").description("Submission that this project is part of"),
                                        fieldWithPath("_embedded.processingStatus").description("Processing status for this project."),
                                        fieldWithPath("_embedded.validationResult").description("Validation result for this project."),
                                        fieldWithPath("team").description("Team this project belongs to"),
                                        fieldWithPath("releaseDate").description("Date at which this project can be released"),
                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        validationresultLink(),
                                        submissionLink(),
                                        processingStatusLink(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("project").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("history").description("Collection of resources for samples with the same team and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by team and alias"),
                                        linkWithRel("dataType").description("Resource describing the requirements for this data type"),
                                        linkWithRel("checklist").description("Resource describing opt-in data requirements for this document")

                                )
                        )
                );

        String projectId = projectRepository.findAll().get(0).getId();

        this.mockMvc.perform(
                put("/api/projects/" + projectId).content(jsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isOk())
                .andDo(
                        document("update-project",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the project within the team"),
                                        fieldWithPath("title").description("Title for the project"),
                                        fieldWithPath("description").description("Description for the project"),
                                        fieldWithPath("contacts").description("Contacts for this project"),
                                        fieldWithPath("publications").description("Publications related to thisproject"),
                                        //fieldWithPath("attributes").description("A list of attributes for the project"),
                                        fieldWithPath("_embedded.submission").description("Submission that this project is part of"),
                                        fieldWithPath("_embedded.processingStatus").description("Processing status for this project."),
                                        fieldWithPath("_embedded.validationResult").description("Validation result for this project."),
                                        fieldWithPath("team").description("Team this project belongs to"),
                                        fieldWithPath("releaseDate").description("Date at which this project can be released"),
                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        validationresultLink(),
                                        submissionLink(),
                                        processingStatusLink(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("project").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("history").description("Collection of resources for samples with the same team and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by team and alias"),
                                        linkWithRel("dataType").description("Resource describing the requirements for this data type"),
                                        linkWithRel("checklist").description("Resource describing opt-in data requirements for this document")

                                )
                        )
                );

        Map<String, String> patchValues = new HashMap<>();
        patchValues.put("title", "Example title for our scientific project, between 50 and 4000 characters long");
        String patchJsonRepresentation = objectMapper.writeValueAsString(patchValues);

        this.mockMvc.perform(
                patch("/api/projects/" + projectId).content(patchJsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isOk())
                .andDo(
                        document("patch-project",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the project within the team"),
                                        fieldWithPath("title").description("Title for the project"),
                                        fieldWithPath("description").description("Description for the project"),
                                        fieldWithPath("contacts").description("Contacts for this project"),
                                        fieldWithPath("publications").description("Publications related to thisproject"),
                                        //fieldWithPath("attributes").description("A list of attributes for the project"),
                                        fieldWithPath("_embedded.submission").description("Submission that this project is part of"),
                                        fieldWithPath("_embedded.processingStatus").description("Processing status for this project."),
                                        fieldWithPath("_embedded.validationResult").description("Validation result for this project."),
                                        fieldWithPath("team").description("Team this project belongs to"),
                                        fieldWithPath("releaseDate").description("Date at which this project can be released"),
                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        validationresultLink(),
                                        submissionLink(),
                                        processingStatusLink(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("project").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("history").description("Collection of resources for samples with the same team and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by team and alias"),
                                        linkWithRel("dataType").description("Resource describing the requirements for this data type"),
                                        linkWithRel("checklist").description("Resource describing opt-in data requirements for this document")

                                )
                        )
                );


        //submission contents should now just have a link to view projects
        this.mockMvc.perform(get("/api/submissions/{submissionId}/contents", sub.getId()))
                .andExpect(status().isOk())
                .andDo(document(
                        "submission-contents-post-project-creation",
                        preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                        preprocessResponse(prettyPrint()),
                        links(
                                halLinks(),
                                linkWithRel("analyses").description("Collection of analyses within this submission"),
                                linkWithRel("analyses:create").description("Create a new analysis resource"),
                                linkWithRel("assayData").description("Collection of assay data within this submission"),
                                linkWithRel("assayData:create").description("Create a new assay data resource"),
                                linkWithRel("assays").description("Collection of assays within this submission"),
                                linkWithRel("assays:create").description("Create a new assay resource"),
                                linkWithRel("egaDacPolicies").description("Collection of DAC policies within this submission"),
                                linkWithRel("egaDacPolicies:create").description("Create a new DAC policy resource"),
                                linkWithRel("egaDacs").description("Collection of DACs within this submission"),
                                linkWithRel("egaDacs:create").description("Create a new DAC resource"),
                                linkWithRel("egaDatasets").description("Collection of EGA Datasets within this submission"),
                                linkWithRel("egaDatasets:create").description("Create a new EGA dataset resource"),
                                linkWithRel("files").description("Collection of files within this submission"),
                                linkWithRel("project").description("View the project for this submission"),
                                linkWithRel("protocols").description("Collection of protocols within this submission"),
                                linkWithRel("protocols:create").description("Create a new protocol resource"),
                                linkWithRel("sampleGroups").description("Collection of sample groups within this submission"),
                                linkWithRel("sampleGroups:create").description("Create a new sample group resource"),
                                linkWithRel("samples").description("Collection of samples within this submission"),
                                linkWithRel("samples:create").description("Create a new sample resource"),
                                linkWithRel("sheetUpload").description("Upload a spreadsheet of submittables, based on a template"),
                                linkWithRel("studies").description("Collection of studies within this submission"),
                                linkWithRel("studies:create").description("Create a new study resource"),
                                linkWithRel("samplesSheets").description("Samples spreadsheets that have been uploaded but not processed")
                        ),
                        responseFields(
                                fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources")
                        )
                ));
    }

    @Test
    public void createSample() throws Exception {
        Submission sub = storeSubmission();
        uk.ac.ebi.subs.data.client.Sample sample = Helpers.generateTestClientSamples(1).get(0);

        String jsonRepresentation = objectMapper.writeValueAsString(sample);

        this.mockMvc.perform(
                post("/api/submissions/" + sub.getId() + "/contents/samples/").content(jsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isCreated())
                .andDo(
                        document("create-sample",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the sample within the team"),
                                        fieldWithPath("title").description("Title for the sample"),
                                        fieldWithPath("description").description("Description for the sample"),
                                        fieldWithPath("attributes").description("A list of attributes for the sample"),
                                        fieldWithPath("sampleRelationships").description("Relationships to other samples"),
                                        fieldWithPath("taxonId").description("NCBI Taxon ID for this sample"),
                                        fieldWithPath("taxon").description("Scientific name for this taxon"),
                                        fieldWithPath("_embedded.submission").description("Submission that this sample is part of"),
                                        fieldWithPath("_embedded.processingStatus").description("Processing status for this sample."),
                                        fieldWithPath("_embedded.validationResult").description("Validation result for this sample."),
                                        fieldWithPath("team").description("Team this sample belongs to"),

                                        fieldWithPath("releaseDate").description("Date at which this sample will be released"),
                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        validationresultLink(),
                                        submissionLink(),
                                        processingStatusLink(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("sample").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("history").description("Collection of resources for samples with the same team and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by team and alias"),
                                        linkWithRel("dataType").description("Resource describing the requirements for this data type"),
                                        linkWithRel("checklist").description("Resource describing opt-in data requirements for this document")
                                )
                        )
                );

        String sampleId = sampleRepository.findAll().get(0).getId();
        SampleRelationship sampleRelationship = new SampleRelationship();
        sampleRelationship.setAlias("D0");
        sampleRelationship.setRelationshipNature("Child of");

        sample.getSampleRelationships().add(sampleRelationship);

        jsonRepresentation = objectMapper.writeValueAsString(sample);

        this.mockMvc.perform(
                put("/api/samples/{id}", sampleId).content(jsonRepresentation)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isOk())
                .andDo(
                        document("update-sample",
                                preprocessRequest(prettyPrint(), addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the sample within the team"),
                                        fieldWithPath("title").description("Title for the sample"),
                                        fieldWithPath("description").description("Description for the sample"),
                                        fieldWithPath("attributes").description("A list of attributes for the sample"),
                                        fieldWithPath("sampleRelationships").description("Relationships to other samples"),
                                        fieldWithPath("taxonId").description("NCBI Taxon ID for this sample"),
                                        fieldWithPath("taxon").description("Scientific name for this taxon"),
                                        fieldWithPath("_embedded.submission").description("Submission that this sample is part of"),
                                        fieldWithPath("_embedded.processingStatus").description("Processing status for this sample."),
                                        fieldWithPath("_embedded.validationResult").description("Validation result for this sample."),
                                        fieldWithPath("team").description("Team this sample belongs to"),

                                        fieldWithPath("releaseDate").description("Date at which this project will be released"),
                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        validationresultLink(),
                                        submissionLink(),
                                        processingStatusLink(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("sample").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("history").description("Collection of resources for samples with the same team and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by team and alias"),
                                        linkWithRel("dataType").description("Resource describing the requirements for this data type"),
                                        linkWithRel("checklist").description("Resource describing opt-in data requirements for this document")
                                )
                        )
                );

        this.mockMvc.perform(
                patch("/api/samples/{id}", sampleId).content("{\"title\":\"New title\"}")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isOk())
                .andDo(
                        document("patch-sample",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the sample within the team"),
                                        fieldWithPath("title").description("Title for the sample"),
                                        fieldWithPath("description").description("Description for the sample"),
                                        fieldWithPath("attributes").description("A list of attributes for the sample"),
                                        fieldWithPath("sampleRelationships").description("Relationships to other samples"),
                                        fieldWithPath("taxonId").description("NCBI Taxon ID for this sample"),
                                        fieldWithPath("taxon").description("Scientific name for this taxon"),
                                        fieldWithPath("_embedded.submission").description("Submission that this sample is part of"),
                                        fieldWithPath("_embedded.processingStatus").description("Processing status for this sample."),
                                        fieldWithPath("_embedded.validationResult").description("Validation result for this sample."),
                                        fieldWithPath("team").description("Team this sample belongs to"),

                                        fieldWithPath("releaseDate").description("Date at which this project will be released"),
                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        validationresultLink(),
                                        submissionLink(),
                                        processingStatusLink(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("sample").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("history").description("Collection of resources for samples with the same team and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by team and alias"),
                                        linkWithRel("dataType").description("Resource describing the requirements for this data type"),
                                        linkWithRel("checklist").description("Resource describing opt-in data requirements for this document")

                                )
                        )
                );

        ValidationResult validationResult = validationResultRepository.findAll().get(0);

        this.mockMvc.perform(
                get("/api/validationResults/{validationResultId}", validationResult.getUuid())
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isOk())
                .andDo(
                        document("get-validation-result",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),

                                        fieldWithPath("validationStatus").description("Is validation pending or complete?"),
                                        fieldWithPath("version").description("Version of this resource.")
                                ),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("validationResult").description("This resource"),
                                        linkWithRel("submittable").description("The submittable this validation result refers to.")

                                )
                        )
                );
    }

    @Test
    public void pageExample() throws Exception {

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

    @Test
    public void conditionalRequests() throws Exception {
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

    @Test
    public void sampleList() throws Exception {
        Submission sub = storeSubmission();
        List<Sample> samples = storeSamples(sub, 30);

        this.mockMvc.perform(
                get("/api/samples/search/by-submission?submissionId={submissionId}&size=2", sub.getId())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("samples/by-submission",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        DocumentationHelper.selfRelLink(),
                                        DocumentationHelper.nextRelLink(),
                                        DocumentationHelper.firstRelLink(),
                                        DocumentationHelper.lastRelLink()
                                ),
                                responseFields(
                                        DocumentationHelper.linksResponseField(),
                                        fieldWithPath("_embedded.samples").description("Samples within the submission"),
                                        DocumentationHelper.paginationBlock()
                                )
                        )
                );

        this.mockMvc.perform(
                get("/api/samples/{sample}", samples.get(0).getId())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("samples/fetch-one",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        DocumentationHelper.selfRelLink(),
                                        processingStatusLink(),
                                        submissionLink(),
                                        validationresultLink(),
                                        linkWithRel("sample").description("Link to this sample"),
                                        linkWithRel("self:update").description("This sample can be updated"),
                                        linkWithRel("self:delete").description("This sample can be deleted"),
                                        linkWithRel("history").description("Collection of resources for samples with the same team and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by team and alias"),
                                        linkWithRel("dataType").description("Resource describing the requirements for this data type"),
                                        linkWithRel("checklist").description("Resource describing opt-in data requirements for this document")
                                ),
                                responseFields( //TODO fill out the descriptions
                                        DocumentationHelper.linksResponseField(),
                                        fieldWithPath("alias").description(""),
                                        fieldWithPath("title").description(""),
                                        fieldWithPath("description").description(""),
                                        fieldWithPath("sampleRelationships").description(""),
                                        fieldWithPath("taxonId").description(""),
                                        fieldWithPath("taxon").description(""),
                                        fieldWithPath("team").description("Team this sample belongs to"),
                                        fieldWithPath("attributes").description(""),
                                        fieldWithPath("createdDate").description(""),
                                        fieldWithPath("lastModifiedDate").description(""),
                                        fieldWithPath("createdBy").description(""),
                                        fieldWithPath("lastModifiedBy").description(""),
                                        fieldWithPath("_embedded.submission").description(""),
                                        fieldWithPath("_embedded.processingStatus").description("")
                                )
                        ));
    }

    @Test
    public void samplesSearchResource() throws Exception {
        this.mockMvc.perform(
                get("/api/samples/search")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("samples-search-resource",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("by-submission").description("Search for all samples within a submission"),
                                        linkWithRel("by-team").description("Search for samples within a team"),
                                        linkWithRel("by-accession").description("Find the current version of a sample by archive accession"),
                                        linkWithRel("by-submissionId-and-alias").description("Search for a sample by alias within a submission"),
                                        linkWithRel("current-version").description("Find the current version of a sample by team and alias"),
                                        linkWithRel("history").description("Search for all versions of a sample by team and alias ")

                                ),
                                responseFields(
                                        DocumentationHelper.linksResponseField()
                                )
                        )
                );
    }

    @Test
    public void rootEndpoint() throws Exception {

        this.mockMvc.perform(
                get("/api/")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("root-endpoint",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(

                                        halLinks(),
                                        //team
                                        linkWithRel("userTeams").description("Collection resource for teams"),
                                        linkWithRel("team").description("Templated link for finding one team"),
                                        //ref data
                                        linkWithRel("studyDataTypes").description("Collection resource for study data types"),
                                        linkWithRel("submissionStatusDescriptions").description("Collection resource for submission status descriptions"),
                                        linkWithRel("processingStatusDescriptions").description("Collection resource for processing status descriptions "),
                                        linkWithRel("releaseStatusDescriptions").description("Collection resource for release status descriptions"),
                                        linkWithRel("uiSupportItems").description("Collection of data to populate help and example text in the UI"),
                                        linkWithRel("uiSupportItems:search").description("Search resource for UI support items"),
                                        linkWithRel("templates").description("Collection of spreadsheet templates for bulk entry of data"),
                                        linkWithRel("templates:search").description("Search resource for spreadsheet templates"),
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
    public void submissionsByTeam() throws Exception {

        Submission sub = storeSubmission();

        this.mockMvc.perform(
                get("/api/submissions/search/by-team?teamName={teamName}", sub.getTeam().getName())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("submissions/by-team",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        DocumentationHelper.selfRelLink()
                                ),
                                responseFields(
                                        DocumentationHelper.linksResponseField(),
                                        fieldWithPath("_embedded.submissions").description("Submissions matching the team name"),
                                        DocumentationHelper.paginationBlock()
                                )
                        )
                );
    }

    @Test
    public void projectsForUser() throws Exception {
        for (int i = 0; i < 3; i++) {
            Submission submission = storeSubmission();
            storeProjects(submission, 1);
        }

        this.mockMvc.perform(
                get("/api/user/projects")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("userProjects",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        DocumentationHelper.selfRelLink()
                                ),
                                responseFields(
                                        DocumentationHelper.linksResponseField(),
                                        fieldWithPath("_embedded.projects").description("Projects available to current user"),
                                        DocumentationHelper.paginationBlock()
                                )
                        )
                );
    }

    @Test
    public void submissionStatusSummaryForUser() throws Exception {
        for (int i = 0; i < 3; i++) {
            storeSubmission();
        }

        this.mockMvc.perform(
                get("/api/user/submissionStatusSummary")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("userSubmissionStatusSummary",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        DocumentationHelper.selfRelLink()
                                ),
                                responseFields(
                                        DocumentationHelper.linksResponseField(),
                                        fieldWithPath("content").description("Number of submissions for each status")
                                )
                        )
                );
    }

    @Test
    public void studyDataTypes() throws Exception {

        this.mockMvc.perform(
                get("/api/studyDataTypes")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("studyDataTypes",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        DocumentationHelper.selfRelLink()
                                ),
                                responseFields(
                                        DocumentationHelper.linksResponseField(),
                                        fieldWithPath("content").description("Study data types and available subtypes")
                                )
                        )
                );
    }

    @Test
    public void submissionsForUser() throws Exception {
        for (int i = 0; i < 3; i++) {
            storeSubmission();

        }

        this.mockMvc.perform(
                get("/api/user/submissions")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("userSubmissions",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        DocumentationHelper.selfRelLink()
                                ),
                                responseFields(
                                        DocumentationHelper.linksResponseField(),
                                        fieldWithPath("_embedded.submissions").description("Submissions available to current user"),
                                        DocumentationHelper.paginationBlock()
                                )
                        )
                );
    }

    private void clearDatabases() {
        this.submissionRepository.deleteAll();
        this.sampleRepository.deleteAll();
        this.submissionStatusRepository.deleteAll();
        this.validationResultRepository.deleteAll();
        this.processingStatusRepository.deleteAll();
        this.projectRepository.deleteAll();
        this.assayRepository.deleteAll();
        this.assayDataRepository.deleteAll();
        this.studyRepository.deleteAll();
    }

    /* Test Helper Methods */

    private uk.ac.ebi.subs.data.Submission goodClientSubmission() {
        uk.ac.ebi.subs.data.Submission submission = new uk.ac.ebi.subs.data.Submission();
        submission.setSubmitter(null);
        submission.setTeam(null);
        return submission;
    }

    private void fakeProcessingStatus(Submission sub) {
        IntStream
                .rangeClosed(1, 10)
                .mapToObj(Integer::valueOf)
                .forEach(index ->
                        storeProcessingStatus(
                                sub,
                                Sample.class,
                                "sample" + index,
                                "SAMEAFAKE0000" + index,
                                Archive.BioSamples,
                                ProcessingStatusEnum.Completed
                        )
                );

        storeProcessingStatus(
                sub,
                Study.class,
                "study",
                null,
                Archive.Ena,
                ProcessingStatusEnum.Dispatched
        );

        IntStream
                .rangeClosed(1, 10)
                .mapToObj(Integer::valueOf)
                .forEach(index ->
                        storeProcessingStatus(
                                sub,
                                Assay.class,
                                "assay" + index,
                                null,
                                Archive.Ena,
                                ProcessingStatusEnum.Dispatched
                        )
                );

        IntStream
                .rangeClosed(1, 10)
                .mapToObj(Integer::valueOf)
                .forEach(index ->
                        storeProcessingStatus(
                                sub,
                                AssayData.class,
                                "assayData" + index,
                                null,
                                Archive.Ena,
                                ProcessingStatusEnum.Dispatched
                        )
                );
    }

    private void storeProcessingStatus(
            Submission sub,
            Class<? extends StoredSubmittable> type,
            String alias,
            String accession,
            Archive archive,
            ProcessingStatusEnum processingStatusEnum
    ) {
        ProcessingStatus status = new ProcessingStatus();
        status.setSubmissionId(sub.getId());
        status.setAccession(accession);
        status.setArchive(archive.name());
        status.setSubmittableType(type.getSimpleName());

        status.setStatus(processingStatusEnum);
        processingStatusRepository.insert(status);
    }

    private uk.ac.ebi.subs.data.Submission badClientSubmission() {
        return new uk.ac.ebi.subs.data.Submission();
    }

    public ContentModifyingOperationPreprocessor maskEmbedded() {
        return new ContentModifyingOperationPreprocessor(new MaskElement("_embedded"));
    }

    public ContentModifyingOperationPreprocessor maskLinks() {
        return new ContentModifyingOperationPreprocessor(new MaskElement("_links"));
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

    private List<Project> storeProjects(Submission sub, int numberRequired) {
        List<Project> projects = Helpers.generateTestProjects(numberRequired);

        for (Project p : projects) {
            p.setCreatedDate(new Date());
            p.setSubmission(sub);
            processingStatusRepository.insert(p.getProcessingStatus());
            projectRepository.insert(p);
        }
        return projects;
    }

    private Submission storeSubmission() {
        Submission sub = Helpers.generateTestSubmission();

        this.submissionStatusRepository.insert(sub.getSubmissionStatus());
        this.submissionRepository.save(sub);
        return sub;
    }

    private LinkDescriptor submissionLink() {
        return linkWithRel("submission").description("Submission in which this record was created");
    }

    private LinkDescriptor processingStatusLink() {
        return linkWithRel("processingStatus").description("Current status of this record");
    }

    private LinkDescriptor validationresultLink() {
        return linkWithRel("validationResult").description("Result of the validation of this record");
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
}
