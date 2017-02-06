package uk.ac.ebi.subs.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.subs.data.status.ProcessingStatus;
import uk.ac.ebi.subs.data.status.ReleaseStatus;
import uk.ac.ebi.subs.data.status.Status;
import uk.ac.ebi.subs.data.status.SubmissionStatus;

import java.util.Arrays;
import java.util.List;


@Configuration
public class StatusConfiguration {

    @Bean
    public List<Status> submissionStatuses() {
        List<Status> statuses = Arrays.asList(
                Status.build(SubmissionStatus.Draft, "In preparation")
                        .addUserTransition(SubmissionStatus.Submitted),

                Status.build(SubmissionStatus.Submitted, "User has submitted documents for storage by archives")
                        .addSystemTransition(SubmissionStatus.Processing),

                Status.build(SubmissionStatus.Processing, "Submission system is processing the submission")
                        .addSystemTransition(SubmissionStatus.Done),

                Status.build(SubmissionStatus.Done, "Submission has been stored in the archives")
        );

        return statuses;
    }


    @Bean
    public List<Status> releaseStatuses() {
        List<Status> statuses = Arrays.asList(
                Status.build(ReleaseStatus.Draft, "In preparation")
                        .addUserTransition(ReleaseStatus.Private),

                Status.build(ReleaseStatus.Private, "Stored in an archive but not released yet")
                        .addUserTransition(ReleaseStatus.Cancelled)
                        .addSystemTransition(ReleaseStatus.Public),

                Status.build(ReleaseStatus.Cancelled, "Exists in an archive but will not be released")
                        .addUserTransition(ReleaseStatus.Private),

                Status.build(ReleaseStatus.Public, "Available through an archive")
                        .addSystemTransition(ReleaseStatus.Suppressed)
                        .addSystemTransition(ReleaseStatus.Killed),

                Status.build(ReleaseStatus.Suppressed, "Data available but not findable without the accession")
                        .addSystemTransition(ReleaseStatus.Public),

                Status.build(ReleaseStatus.Killed, "Data not available and metadata not findable without the accession")

        );

        return statuses;
    }

    @Bean
    public List<Status> processingStatuses() {
        List<Status> statuses = Arrays.asList(

                Status.build(ProcessingStatus.Draft, "In preparation")
                        .addUserTransition(ProcessingStatus.Submitted),

                Status.build(ProcessingStatus.Submitted, "User has submitted document for storage by archives")
                        .addSystemTransition(ProcessingStatus.Dispatched),

                Status.build(ProcessingStatus.Dispatched, "Submission system has dispatched document to the archive")
                        .addSystemTransition(ProcessingStatus.Received),

                Status.build(ProcessingStatus.Received, "Archive has received document")
                        .addSystemTransition(ProcessingStatus.Curation)
                        .addSystemTransition(ProcessingStatus.Processing),

                Status.build(ProcessingStatus.Curation, "Curation team is reviewing document")
                        .addSystemTransition(ProcessingStatus.Accepted)
                        .addSystemTransition(ProcessingStatus.ActionRequired),

                Status.build(ProcessingStatus.Accepted, "Curation team has accepted document")
                        .addSystemTransition(ProcessingStatus.Processing),

                Status.build(ProcessingStatus.ActionRequired, "Curation team have requested changes or additional information")
                        .addUserTransition(ProcessingStatus.Submitted),

                Status.build(ProcessingStatus.Processing, "Archive is processing document")
                        .addSystemTransition(ProcessingStatus.Done),

                Status.build(ProcessingStatus.Done, "Archive has stored document")
        );


        return statuses;
    }

}
