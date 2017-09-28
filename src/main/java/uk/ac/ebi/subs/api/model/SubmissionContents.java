package uk.ac.ebi.subs.api.model;

import uk.ac.ebi.subs.repository.model.Submission;

public class SubmissionContents {
    private Submission submission;

    public SubmissionContents(Submission submission) {
        this.submission = submission;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }
}
