package uk.ac.ebi.subs.api.model;

import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.Submission;

import java.util.List;

/**
 * Value object storing the list of data types of a given {@link Submission}.
 */
public class SubmissionContents {
    private Submission submission;

    private List<DataType> dataTypes;

    public SubmissionContents(Submission submission) {
        this.submission = submission;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public List<DataType> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(List<DataType> dataTypes) {
        this.dataTypes = dataTypes;
    }
}
