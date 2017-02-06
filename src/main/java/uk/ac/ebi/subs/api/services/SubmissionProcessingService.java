package uk.ac.ebi.subs.api.services;


import uk.ac.ebi.subs.data.Submission;

/**
 * Send a submission off to be processed
 */
public interface SubmissionProcessingService {

    void submitSubmissionForProcessing(Submission submission);

    void deleteSubmissionContents(Submission submission);
}
