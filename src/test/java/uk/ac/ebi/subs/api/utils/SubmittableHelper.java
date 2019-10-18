package uk.ac.ebi.subs.api.utils;

import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.model.AssayData;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;

import java.util.UUID;

public class SubmittableHelper {

    public final static String TEAM_NAME = "subs.dev-team-1";
    private static final String SAMPLE_DESCRIPTION = "Human sample donor";
    private static final String TAXON = "Homo sapiens";
    private static final long TAXON_ID = 9606L;

    public static AssayData createAssayData(String submissionID) {
        AssayData assayData = new AssayData();
        assayData.setId(UUID.randomUUID().toString());
        assayData.setAlias("Test assay data");
        assayData.setTeam(generateTestTeam());
        assayData.setSubmission(createSubmission(submissionID));

        return assayData;
    }

    public static Submission createSubmission(String submissionID) {
        Submission submission = new Submission();
        submission.setId(submissionID);
        submission.setTeam(generateTestTeam());

        return submission;
    }

    public static SubmissionStatus createSubmissionStatus() {
        final SubmissionStatus submissionStatus = new SubmissionStatus();
        submissionStatus.setStatus(SubmissionStatusEnum.Draft);
        submissionStatus.setTeam(SubmittableHelper.generateTestTeam());

        return submissionStatus;
    }

    public static Sample createSample(boolean createProcessingStatus, String sampleAlias) {
        Sample sample = new Sample();

        sample.setId(SubmittableHelper.createId());
        sample.setTeam(generateTestTeam());
        if (sampleAlias == null) sampleAlias = createId();
        sample.setAlias("Alias_" + sampleAlias);
        sample.setTitle("Donor_ " + createId());
        sample.setDescription(SAMPLE_DESCRIPTION);
        sample.setTaxon(TAXON);
        sample.setTaxonId(TAXON_ID);

        if (createProcessingStatus) {
            sample.setProcessingStatus(new ProcessingStatus(ProcessingStatusEnum.Draft));
        }

        return sample;
    }

    public static Team generateTestTeam() {
        Team team = new Team();
        team.setName(TEAM_NAME);

        return team;
    }

    public static DataType generateDataType(Archive archive, String id, String submittableClassName) {
        DataType samplesDataType = new DataType();
        samplesDataType.setArchive(archive);
        samplesDataType.setId(id);
        samplesDataType.setSubmittableClassName(submittableClassName);

        return samplesDataType;
    }

    public static Checklist generateChecklist(String checklistId, String dataTypeId) {
        Checklist checklist = new Checklist();
        checklist.setId(checklistId);
        checklist.setDataTypeId(dataTypeId);

        return checklist;
    }

    public static String createId() {
        return UUID.randomUUID().toString();
    }
}
