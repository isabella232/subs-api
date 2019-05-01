package uk.ac.ebi.subs.api.services;


import uk.ac.ebi.subs.processing.SubmissionEnvelope;
import uk.ac.ebi.subs.repository.model.Submission;

/**
 * Send a submission off to be processed
 */
public interface SubmissionEventService {

    void submissionCreated(Submission submission);

    void submissionUpdated(Submission submission);

    void submissionDeleted(Submission submission);

    void submissionSubmitted(SubmissionEnvelope submissionEnvelope);

}
