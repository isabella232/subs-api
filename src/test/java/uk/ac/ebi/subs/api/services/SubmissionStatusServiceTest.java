package uk.ac.ebi.subs.api.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.api.utils.SubmittableHelper;
import uk.ac.ebi.subs.data.status.StatusDescription;

import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SubmissionStatusServiceTest {

    private SubmissionStatusService submissionStatusService;

    @Autowired
    private Map<String, StatusDescription> submissionStatusDescriptionMap;

    @MockBean
    private ValidationResultService validationResultService;

    @MockBean
    private FileService fileService;

    @MockBean
    private SubmissionRepository submissionRepository;

    private static final String SUBMISSION_ID = "1234567890";

    @Before
    public void setup() {
        when(validationResultService.isValidationFinishedAndPassed(any(String.class))).thenReturn(true);
        when(fileService.allFilesBySubmissionIDReadyForArchive(any(String.class))).thenReturn(true);

        submissionStatusService = new SubmissionStatusService(validationResultService, fileService, submissionRepository);
    }

    @Test
    public void testAvailableStatusName() {
        Submission submission = SubmittableHelper.createSubmission(SUBMISSION_ID);
        SubmissionStatus submissionStatus = new SubmissionStatus();
        submissionStatus.setId(UUID.randomUUID().toString());
        submissionStatus.setStatus(SubmissionStatusEnum.Draft);
        submission.setSubmissionStatus(submissionStatus);
        assertThat(
                submissionStatusService.getAvailableStatusNames(submission, submissionStatusDescriptionMap),
                hasItem(SubmissionStatusEnum.Submitted.name())
        );
    }
}
