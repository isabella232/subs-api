package uk.ac.ebi.subs.api;


import uk.ac.ebi.subs.data.client.*;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.repository.model.*;
import uk.ac.ebi.subs.data.component.Domain;
import uk.ac.ebi.subs.data.component.Submitter;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.model.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Helpers {

    public static Submission generateSubmission() {
        Submission s = new Submission();

        Domain d = new Domain();
        d.setName("test domain");
        s.setDomain(d);

        Submitter u = new Submitter();
        u.setEmail("test@test.org");
        s.setSubmitter(u);

        return s;
    }

    public static List<Sample> generateTestSamples() {
        return generateTestSamples(2);
    }

    public static List<uk.ac.ebi.subs.data.client.Sample> generateTestClientSamples(int numberOfSamplesRequired){
        List<uk.ac.ebi.subs.data.client.Sample> samples = new ArrayList<>(numberOfSamplesRequired);

        for (int i = 1; i <= numberOfSamplesRequired; i++){
            uk.ac.ebi.subs.data.client.Sample s = new uk.ac.ebi.subs.data.client.Sample();
            samples.add(s);

            s.setAlias("D"+i);
            s.setTitle("Donor "+i);
            s.setDescription("Human sample donor");
            s.setTaxon("Homo sapiens");
            s.setTaxonId(9606L);

        }

        return samples;
    }


    public static List<Sample> generateTestSamples(int numberOfSamplesRequired){
        List<Sample> samples = new ArrayList<>(numberOfSamplesRequired);

        for (int i = 1; i <= numberOfSamplesRequired; i++){
            Sample s = new Sample();
            samples.add(s);

            s.setAlias("D"+i);
            s.setTitle("Donor "+i);
            s.setDescription("Human sample donor");
            s.setTaxon("Homo sapiens");
            s.setTaxonId(9606L);

            s.setProcessingStatus(new ProcessingStatus(ProcessingStatusEnum.Draft));

        }

        return samples;
    }

    public static Domain generateTestDomain(){
        Domain d = new Domain();
        d.setName("my-domain");
        return d;
    }


    public static Submission generateTestSubmission() {
        Submission sub = new Submission();
        Domain d = new Domain();
        sub.setId(UUID.randomUUID().toString());

        sub.setDomain( generateTestDomain() );

        sub.setSubmissionStatus(new SubmissionStatus(SubmissionStatusEnum.Draft));

        return sub;
    }
}
