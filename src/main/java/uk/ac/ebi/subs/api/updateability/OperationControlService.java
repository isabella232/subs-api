package uk.ac.ebi.subs.api.updateability;


import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;

public interface OperationControlService {

    boolean isUpdateable(Submission submission);

    boolean isUpdateable(StoredSubmittable storedSubmittable);

    boolean isUpdateable(SubmissionStatus submissionStatus);


}
