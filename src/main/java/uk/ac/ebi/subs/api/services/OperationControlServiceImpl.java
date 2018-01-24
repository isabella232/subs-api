package uk.ac.ebi.subs.api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.data.status.StatusDescription;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;

import java.util.Map;

@Service
public class OperationControlServiceImpl implements OperationControlService {

    private Map<String, StatusDescription> submissionStatusDescriptionMap;
    private Map<String, StatusDescription> processingStatusDescriptionMap;

    @Autowired
    public OperationControlServiceImpl(
            Map<String, StatusDescription> submissionStatusDescriptionMap,
            Map<String, StatusDescription> processingStatusDescriptionMap
    ) {
        this.submissionStatusDescriptionMap = submissionStatusDescriptionMap;
        this.processingStatusDescriptionMap = processingStatusDescriptionMap;
    }

    @Override
    public boolean isUpdateable(Submission submission) {
        Assert.notNull(submission);
        Assert.notNull(submission.getSubmissionStatus());

        SubmissionStatus status = submission.getSubmissionStatus();
        return this.isUpdateable(status);
    }

    @Override
    public boolean isUpdateable(StoredSubmittable storedSubmittable) {
        Assert.notNull(storedSubmittable);
        Assert.notNull(storedSubmittable.getSubmission());

        if (storedSubmittable.getProcessingStatus() != null) {
            return this.isUpdateable(storedSubmittable.getProcessingStatus()) && this.isUpdateable(storedSubmittable.getSubmission());
        } else {
            return this.isUpdateable(storedSubmittable.getSubmission());
        }
    }

    public boolean isUpdateable(ProcessingStatus processingStatus){
        Assert.notNull(processingStatus.getStatus());

        StatusDescription statusDescription = processingStatusDescriptionMap.get(processingStatus.getStatus());
        Assert.notNull(statusDescription);

        return statusDescription.isAcceptingUpdates();
    }

    @Override
    public boolean isUpdateable(SubmissionStatus status) {
        Assert.notNull(status);
        Assert.notNull(status.getStatus());

        StatusDescription statusDescription = submissionStatusDescriptionMap.get(status.getStatus());

        Assert.notNull(statusDescription);

        return statusDescription.isAcceptingUpdates();
    }
}
