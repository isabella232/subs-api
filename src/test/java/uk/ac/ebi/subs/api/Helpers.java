package uk.ac.ebi.subs.api;


import uk.ac.ebi.subs.data.component.Domain;
import uk.ac.ebi.subs.data.component.Submitter;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Helpers {

    public static Submission generateSubmission() {
        Submission s = new Submission();

        s.setDomain(generateTestDomain());
        s.setSubmitter(generateTestSubmitter());

        return s;
    }

    private static Submitter generateTestSubmitter() {
        Submitter u = new Submitter();
        u.setEmail("test@test.org");
        return u;
    }

    public static List<Sample> generateTestSamples() {
        return generateTestSamples(2);
    }

    public static List<uk.ac.ebi.subs.data.client.Sample> generateTestClientSamples(int numberOfSamplesRequired) {
        List<uk.ac.ebi.subs.data.client.Sample> samples = new ArrayList<>(numberOfSamplesRequired);

        for (int i = 1; i <= numberOfSamplesRequired; i++) {
            uk.ac.ebi.subs.data.client.Sample s = new uk.ac.ebi.subs.data.client.Sample();
            samples.add(s);

            s.setAlias("D" + i);
            s.setTitle("Donor " + i);
            s.setDescription("Human sample donor");
            s.setTaxon("Homo sapiens");
            s.setTaxonId(9606L);

        }

        return samples;
    }


    public static List<Sample> generateTestSamples(int numberOfSamplesRequired) {
        List<Sample> samples = new ArrayList<>(numberOfSamplesRequired);

        for (int i = 1; i <= numberOfSamplesRequired; i++) {
            Sample s = new Sample();
            samples.add(s);

            s.setId(createId());

            s.setAlias("D" + i);
            s.setTitle("Donor " + i);
            s.setDescription("Human sample donor");
            s.setTaxon("Homo sapiens");
            s.setTaxonId(9606L);

            s.setProcessingStatus(new ProcessingStatus(ProcessingStatusEnum.Draft));

        }

        return samples;
    }

    public static Domain generateTestDomain() {
        Domain d = new Domain();
        d.setName("my-domain");
        return d;
    }


    public static Submission generateTestSubmission() {
        Submission sub = new Submission();
        Domain d = new Domain();
        sub.setId(createId());

        sub.setDomain(generateTestDomain());

        sub.setSubmissionStatus(new SubmissionStatus(SubmissionStatusEnum.Draft));

        return sub;
    }

    private static String createId() {
        return UUID.randomUUID().toString();
    }
}
