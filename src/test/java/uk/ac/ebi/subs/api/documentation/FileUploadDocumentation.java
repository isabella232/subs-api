package uk.ac.ebi.subs.api.documentation;

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
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.DocumentationProducer;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.api.handlers.FileDeletionEventHandler;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.addAuthTokenHeader;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Category(DocumentationProducer.class)
@WithMockUser(username = "file_upload_docs_usi_user", roles = {Helpers.TEAM_NAME})
public class FileUploadDocumentation {

    @Rule
    public final JUnitRestDocumentation restDocumentation = DocumentationHelper.jUnitRestDocumentation();

    @Value("${usi.docs.hostname:localhost}")
    private String host;
    @Value("${usi.docs.port:8080}")
    private int port;
    @Value("${usi.docs.scheme:http}")
    private String scheme;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;
    @Autowired
    private ValidationResultRepository validationResultRepository;

    @MockBean
    private FileDeletionEventHandler fileDeletionEventHandler;

    private MockMvc mockMvc;
    private Submission submission;

    private static final String[] FILENAMES = {"filename1.cram", "filename2.cram", "filename3.cram"};

    private List<File> storedFiles = new ArrayList<>();

    @Before
    public void setUp() {
        clearDatabases();
        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);
        this.submission = storeSubmission();

        for (String filename: FILENAMES) {
            storedFiles.add(createAndStoreFile(filename, this.submission.getId()));
        }
    }

    @After
    public void tearDown() {
        clearDatabases();
    }

    @Test
    public void file() throws Exception {
        this.mockMvc.perform(
                get("/api/files/{fileID}", storedFiles.get(0).getId())
                .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document( "get-file",
                                preprocessRequest(prettyPrint(), addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        subsectionWithPath("_links").description("Links"),
                                        fieldWithPath("generatedTusId").description("Unique ID generated by the TUS server"),
                                        fieldWithPath("filename").description("The name of the uploaded file"),
                                        fieldWithPath("uploadPath").description("The path the file has been uploaded to"),
                                        fieldWithPath("targetPath").description("The path the file has been stored"),
                                        fieldWithPath("submissionId").description("The ID of the submission the file belongs to"),
                                        fieldWithPath("totalSize").description("The total size of the file"),
                                        fieldWithPath("uploadedSize").description("The currently uploaded size of the file"),
                                        fieldWithPath("createdBy").description("The user who has uploaded the file"),
                                        fieldWithPath("uploadStartDate").description("Date the file upload started"),
                                        fieldWithPath("uploadFinishDate").description("Date the file upload finished"),
                                        fieldWithPath("status").description("The status of the file upload process"),
                                        fieldWithPath("checksum").description("The calculated checksum of the file"),
                                        subsectionWithPath("_embedded.validationResult").description("Validation result for this study.")
                                ),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("file").description("Link to the uploaded file resource"),
                                        linkWithRel("validationResult").description("Link to the validation result of this uploaded file")
                                )
                        )
                );
    }

    @Test
    public void filesBySubmissionID() throws Exception {
        this.mockMvc.perform(
                get("/api/files/search/by-submission?submissionId={submissionID}", submission.getId())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
        .andDo(
                document( "get-files-by-submissionID",
                        preprocessRequest(prettyPrint(), addAuthTokenHeader()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                subsectionWithPath("_links").description("<<resources-page-links,Links>> to other resources"),
                                subsectionWithPath("_embedded").description("The list of resources"),
                                subsectionWithPath("_embedded.files[].generatedTusId").description("Unique ID generated by the TUS server"),
                                subsectionWithPath("_embedded.files[].filename").description("The name of the uploaded file"),
                                subsectionWithPath("_embedded.files[].uploadPath").description("The path the file has been uploaded to"),
                                subsectionWithPath("_embedded.files[].targetPath").description("The path the file has been stored"),
                                subsectionWithPath("_embedded.files[].submissionId").description("The ID of the submission the file belongs to"),
                                subsectionWithPath("_embedded.files[].totalSize").description("The total size of the file"),
                                subsectionWithPath("_embedded.files[].uploadedSize").description("The currently uploaded size of the file"),
                                subsectionWithPath("_embedded.files[].createdBy").description("The user who has uploaded the file"),
                                subsectionWithPath("_embedded.files[].uploadStartDate").description("Date the file upload started"),
                                subsectionWithPath("_embedded.files[].uploadFinishDate").description("Date the file upload finished"),
                                subsectionWithPath("_embedded.files[].status").description("The status of the file upload process"),
                                subsectionWithPath("_embedded.files[].checksum").description("The calculated checksum of the file"),
                                subsectionWithPath("_embedded.files[]._embedded.validationResult").description("Validation result for this study."),
                                subsectionWithPath("page.size").description("The number of resources in this page"),
                                subsectionWithPath("page.totalElements").description("The total number of resources"),
                                subsectionWithPath("page.totalPages").description("The total number of pages"),
                                subsectionWithPath("page.number").description("The page number")
                        ),
                        links(
                                halLinks(),
                                linkWithRel("self").description("This resource")
                        )
                )
        );
    }

    @Test
    public void deleteFile() throws Exception {
        this.mockMvc.perform(
                delete("/api/files/{fileID}", storedFiles.get(0).getId())
        ).andExpect(status().isNoContent())
        .andDo(
            document( "delete-file",
                preprocessRequest(prettyPrint(), addAuthTokenHeader()),
                preprocessResponse(prettyPrint())
            )
        );
    }

    private void clearDatabases() {
        this.submissionRepository.deleteAll();
        this.submissionStatusRepository.deleteAll();
        this.fileRepository.deleteAll();
        this.validationResultRepository.deleteAll();
    }

    private Submission storeSubmission() {
        Submission sub = Helpers.generateTestSubmission();

        this.submissionStatusRepository.insert(sub.getSubmissionStatus());
        this.submissionRepository.save(sub);
        return sub;
    }

    private File createAndStoreFile(String filename, String submissionID) {
        File file = Helpers.generateFileWithFileName(filename, submissionID);

        ValidationResult validationResult = Helpers.generateTestValidationResult(submissionID);
        validationResultRepository.save(validationResult);

        file.setValidationResult(validationResult);

        return this.fileRepository.save(file);
    }



}
