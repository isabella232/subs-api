package uk.ac.ebi.subs.api.updateability;


import uk.ac.ebi.subs.data.Submission;

public interface OperationControlService {

    boolean isUpdateable(Submission submission);

    boolean isSubmissionUpdateable(String submissionId);


}
