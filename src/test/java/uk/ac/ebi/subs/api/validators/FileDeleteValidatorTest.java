package uk.ac.ebi.subs.api.validators;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.api.CoreValidatorTest;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.data.component.Submitter;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.model.fileupload.FileStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.services.SubmissionHelperService;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@WithMockUser(username = "usi_user", roles = {CoreValidatorTest.TEST_TEAM_1, Helpers.ADMIN_TEAM_NAME})
public class FileDeleteValidatorTest {

    @Autowired
    private FileDeleteValidator fileDeleteValidator;
    @Autowired
    private SubmissionHelperService submissionHelperService;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;

    private Team team = Team.build(CoreValidatorTest.TEST_TEAM_1);
    private Submitter submitter = Submitter.build("test@test.ac.uk");

    private static final String TUS_ID = UUID.randomUUID().toString();
    private static final String SUBMISSION_ID = UUID.randomUUID().toString();
    private static final String FILENAME = "test.cram";
    private static final long TOTAL_SIZE = 123456L;

    private Submission submission;
    private File file;

    @Before
    public void setup() {
        submission = submissionHelperService.createSubmission(team,submitter);

        file = createFile(submission.getId());
        fileRepository.save(file);
    }

    @Test
    public void whenSubmissionIdIsNullOnFile_ThenValidationErrors() {
        file.setSubmissionId(null);

        Errors errors = new BeanPropertyBindingResult(file, "file");
        fileDeleteValidator.validate(file, errors);

        FieldError statusFieldError = errors.getFieldError("submissionId");

        assertEquals(1, errors.getErrorCount());
        assertThat(statusFieldError, notNullValue());
        assertThat(statusFieldError.getDefaultMessage(), is(equalTo(SubsApiErrors.missing_field.name())));
    }

    @Test
    public void whenFileStatusIsNotDeletable_ThenValidationErrors() {
        file.setStatus(FileStatus.INITIALIZED);

        Errors errors = new BeanPropertyBindingResult(file, "file");
        fileDeleteValidator.validate(file, errors);

        FieldError statusFieldError = errors.getFieldError("status");

        assertEquals(1, errors.getErrorCount());
        assertThat(statusFieldError, notNullValue());
        assertThat(statusFieldError.getDefaultMessage(), is(equalTo(SubsApiErrors.file_is_not_in_deletable_status.name())));
    }

    @Test
    public void whenSubmissionIsNotModifiable_ThenValidationErrors() {
        file.setStatus(FileStatus.READY_FOR_ARCHIVE);

        SubmissionStatus submissionStatus = new SubmissionStatus(SubmissionStatusEnum.Submitted);
        submission.setSubmissionStatus(submissionStatus);
        submissionStatusRepository.insert(submissionStatus);
        submissionRepository.save(submission);

        Errors errors = new BeanPropertyBindingResult(file, "file");
        fileDeleteValidator.validate(file, errors);

        assertEquals(1, errors.getErrorCount());
        assertThat(errors.getGlobalError().getDefaultMessage(), is(equalTo(SubsApiErrors.resource_locked.name())));
    }

    public static File createFile(String submissionId) {
        File file = new File();
        file.setGeneratedTusId(TUS_ID);
        file.setSubmissionId(submissionId);
        file.setFilename(FILENAME);
        file.setTotalSize(TOTAL_SIZE);

        return file;
    }

}
