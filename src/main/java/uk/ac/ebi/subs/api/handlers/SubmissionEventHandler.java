package uk.ac.ebi.subs.api.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.services.SubmissionEventService;
import uk.ac.ebi.subs.data.component.Submitter;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.services.SubmissionHelperService;
import uk.ac.ebi.tsc.aap.client.model.User;

/**
 * Repo event handler for submissions in the api.
 * <p>
 * Locks down changes to non-draft submissions, based on the {@link uk.ac.ebi.subs.api.services.OperationControlService}.
 * Send submissions off to rabbit after storing a submission with the 'Submitted' status.
 */
@Component
@RepositoryEventHandler
public class SubmissionEventHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String DEFAULT_USER_EMAIL = "alice@example.com";
    private static final String DEFAULT_USER_NAME = "no name";

    public SubmissionEventHandler(
            SubmissionRepository submissionRepository,
            SubmissionEventService submissionEventService,
            SubmissionHelperService submissionHelperService
    ) {
        this.submissionEventService = submissionEventService;
        this.submissionRepository = submissionRepository;
        this.submissionHelperService = submissionHelperService;
    }


    private SubmissionRepository submissionRepository;
    private SubmissionEventService submissionEventService;
    private SubmissionHelperService submissionHelperService;

    public void setSubmissionRepository(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public void setSubmissionEventService(SubmissionEventService submissionEventService) {
        this.submissionEventService = submissionEventService;
    }

    public void setSubmissionHelperService(SubmissionHelperService submissionHelperService) {
        this.submissionHelperService = submissionHelperService;
    }

    /**
     * Give submission an ID and draft status on creation
     *
     * @param submission
     */
    @HandleBeforeCreate
    public void handleBeforeCreate(Submission submission) {
        setSubmitterEmailOnSubmission(submission);
        submissionHelperService.setupNewSubmission(submission);
        submissionEventService.submissionCreated(submission);
    }

    /**
     * make sure the submission is ready for storing
     * give it an ID if it has not got one
     * check it can be modified if there it already exists
     *
     * @param submission
     */
    @HandleBeforeSave
    public void handleBeforeSave(Submission submission) {
        Submission storedSubmission = submissionRepository.findById(submission.getId()).orElse(null);
        submission.setSubmissionStatus(storedSubmission.getSubmissionStatus());

        submissionEventService.submissionUpdated(submission);
    }

    @HandleAfterDelete
    public void handleBeforeDelete(Submission submission) {
        submissionEventService.submissionDeleted(submission);
    }

    private void setSubmitterEmailOnSubmission(Submission submission) {
        Submitter submitter = getSubmitterFromLoggedInUser();
        submission.setSubmitter(submitter);
    }

    private Submitter getSubmitterFromLoggedInUser() {
        String email = DEFAULT_USER_EMAIL;
        String name = DEFAULT_USER_NAME;

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {

            final Object details = authentication.getDetails();
            if (details instanceof User) {
                User user = (User) details;
                email = user.getEmail();
                name = user.getFullName();
            }
        }

        Submitter s = new Submitter();
        s.setEmail(email);
        s.setName(name);
        return s;
    }
}
