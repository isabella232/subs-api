package uk.ac.ebi.subs.api.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.api.ApiIntegrationTestHelper;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.validator.data.SingleValidationResult;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.data.structures.GlobalValidationStatus;
import uk.ac.ebi.subs.validator.data.structures.SingleValidationResultStatus;
import uk.ac.ebi.subs.validator.data.structures.ValidationAuthor;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;
import uk.ac.ebi.subs.validator.repository.ValidatorResultRepositoryCustom;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileRepositoryRest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.subs.api.utils.ValidationResultHelper.generateExpectedResults;
import static uk.ac.ebi.subs.api.utils.ValidationResultHelper.generateValidationResult;

@RunWith(SpringRunner.class)
@WebMvcTest(SubmissionBlockersSummaryController.class)
@WithMockUser(username = "usi_admin_user", roles = {Helpers.TEAM_NAME})
public class SubmissionBlockersSummaryControllerTest {

    private static final String SUBMISSION_ID = "111-222-3333";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private DomainService domainService;
    @MockBean
    private ProfileRepositoryRest profileRepositoryRest;

    @MockBean
    private ValidationResultRepository validationResultRepository;

    @MockBean
    private ProcessingStatusRepository processingStatusRepository;

    @MockBean
    FileRepository fileRepository;

    @MockBean
    ValidatorResultRepositoryCustom validatorResultRepositoryCustom;

    private List<File> files;

    @Before
    public void setup() {
        this.files = new ArrayList<>();
        generateFiles();

        this.mvc = MockMvcBuilders.webAppContextSetup(context)
                .defaultRequest(RestDocumentationRequestBuilders.get("/").contextPath("/api"))
                .build();

        ApiIntegrationTestHelper.mockAapProfileAndDomain(domainService, profileRepositoryRest);

        given(fileRepository.countBySubmissionIdAndStatusNot(SUBMISSION_ID, FileStatus.READY_FOR_ARCHIVE))
                .willReturn(3L);

        Map<ValidationAuthor, List<SingleValidationResult>> validationResultByValidationAuthors = new HashMap<>();
        validationResultByValidationAuthors.putAll(generateExpectedResults(
                Arrays.asList(SingleValidationResultStatus.Pass, SingleValidationResultStatus.Error, SingleValidationResultStatus.Warning)));

        ValidationResult validationResult = generateValidationResult(validationResultByValidationAuthors);
        validationResult.setValidationStatus(GlobalValidationStatus.Pending);

        given(validationResultRepository.countBySubmissionIdAndValidationStatusIs(SUBMISSION_ID, GlobalValidationStatus.Pending))
            .willReturn(1L);
    }

    @Test
    public void given2FilesRelatedToTheSubmissionAreNotInReady_to_Archive_Status__Returns2FilesUploadingMessage()
            throws Exception {
        mvc.perform(get(String.format("/api/submissions/%s/submissionBlockersSummary", SUBMISSION_ID))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notReadyFileCount", is(equalTo(3))));
    }

    @Test
    public void given4MetadataIssuesToResolve__ReturnsMessageWith4MetadataIssues()
            throws Exception {
        given(validatorResultRepositoryCustom.validationIssuesPerDataTypeId(any()))
                .willReturn(generateMetadataIssues());

        mvc.perform(get(String.format("/api/submissions/%s/submissionBlockersSummary", SUBMISSION_ID))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationIssuesPerDataTypeId.samples", is(equalTo(3))))
                .andExpect(jsonPath("$.validationIssuesPerDataTypeId.studies", is(equalTo(1))));
    }

    @Test
    public void givenEmptySubmission_ReturnsEmptySubmissionMessage()
            throws Exception {
        given(validationResultRepository.findAllBySubmissionId(any()))
                .willReturn(Collections.emptyList());

        given(processingStatusRepository.findBySubmissionId(any()))
                .willReturn(Collections.emptyList());

        mvc.perform(get(String.format("/api/submissions/%s/submissionBlockersSummary", SUBMISSION_ID))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emptySubmission", is(equalTo(true))));
    }

    @Test
    public void givenSubmissionsValidationResultPending_ReturnsValidationResultPendinghnMessage()
            throws Exception {
        mvc.perform(get(String.format("/api/submissions/%s/submissionBlockersSummary", SUBMISSION_ID))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anyPendingValidationResult", is(equalTo(true))));
    }

    private Map<String, Integer> generateMetadataIssues() {
        Map<String, Integer> metadataIssues = new HashMap<>();
        metadataIssues.put("samples", 3);
        metadataIssues.put("studies", 1);

        return metadataIssues;
    }

    private void generateFiles() {
        files.add(generateFile(FileStatus.READY_FOR_ARCHIVE));
        files.add(generateFile(FileStatus.UPLOADING));
        files.add(generateFile(FileStatus.UPLOADED));
        files.add(generateFile(FileStatus.READY_FOR_CHECKSUM));
    }

    private File generateFile(FileStatus fileStatus) {
        File file = new File();
        file.setId(UUID.randomUUID().toString());
        file.setSubmissionId(SUBMISSION_ID);
        file.setStatus(fileStatus);

        return file;
    }



}
