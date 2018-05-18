package uk.ac.ebi.subs.api.utils;

import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.AssayData;
import uk.ac.ebi.subs.repository.model.Submission;

import java.util.UUID;

public class SubmittableHelper {

    public static AssayData createAssayData(String submissionID) {
        AssayData assayData = new AssayData();
        assayData.setId(UUID.randomUUID().toString());
        assayData.setAlias("Test assay data");
        assayData.setTeam(getTeam());
        assayData.setSubmission(createSubmission(submissionID));

        return assayData;
    }

    public static Team getTeam() {
        Team team = new Team();
        team.setName("Test team");

        return team;
    }

    public static Submission createSubmission(String submissionID) {
        Submission submission = new Submission();
        submission.setId(submissionID);

        return submission;
    }
}
