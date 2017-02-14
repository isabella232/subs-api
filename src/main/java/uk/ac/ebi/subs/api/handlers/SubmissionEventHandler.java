package uk.ac.ebi.subs.api.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.*;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.api.exceptions.ResourceLockedException;
import uk.ac.ebi.subs.api.services.SubmissionProcessingService;
import uk.ac.ebi.subs.api.updateability.OperationControlService;
import uk.ac.ebi.subs.repository.SubmissionRepository;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionStatusRepository;

import java.util.Date;
import java.util.UUID;

/**
 * Repo event handler for submissions in the api
 * <p>
 * * locks down changes to non-draft submissions, based on the OperationControlService
 * * send submissions off to rabbit after storing a submission with the 'Submitted' status
 */
@Component
@RepositoryEventHandler(Submission.class)
public class SubmissionEventHandler {


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OperationControlService operationControlService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionProcessingService submissionProcessingService;

    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;

    /**
     * Give submission an ID and draft status on creation
     * @param submission
     */
    @HandleBeforeCreate
    public void handleBeforeCreate(Submission submission) {
        submission.setId(UUID.randomUUID().toString());
        submission.setCreatedDate(new Date());

        SubmissionStatus submissionStatus = new SubmissionStatus(submission,SubmissionStatusEnum.Draft);
        submissionStatusRepository.save(submissionStatus);
    }

    /**
     * make sure the submission is ready for storing
     * * give it an ID if it has not got one
     * * check it can be modified if there it already exists
     *
     * @param submission
     */
    @HandleBeforeSave
    public void handleBeforeSave(Submission submission) {

        Submission storedSubmission = submissionRepository.findOne(submission.getId());

        if (storedSubmission != null) {
            if (!operationControlService.isUpdateable(storedSubmission)) {
                throw new ResourceLockedException();
            }
        }
/*TODO fix in SUBS-333
        if (submission.getStatus() != null && submission.getStatus().equals(ProcessingStatusEnum.Submitted.name())){
            submission.setSubmissionDate(new Date());
        }
*/
    }


    /**
     * Once the submission has been stored, if it has a status of submitted, submit it for processing
     *
     * @param submission
     */
    @HandleAfterCreate
    @HandleAfterSave
    public void handleAfterCreateOrSave(Submission submission) {
        logger.warn("after");
        /* TODO fix in SUBS-333
        if (submission.getStatus() != null && submission.getStatus().equals(ProcessingStatusEnum.Submitted.name())) {
            submissionProcessingService.submitSubmissionForProcessing(submission);
        }
        */
    }

    @HandleBeforeDelete
    public void handleBeforeDelete(Submission submission) {
        submissionProcessingService.deleteSubmissionContents(submission);
    }


}
