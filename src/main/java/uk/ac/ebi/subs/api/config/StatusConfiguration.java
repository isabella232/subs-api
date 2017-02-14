package uk.ac.ebi.subs.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.status.ReleaseStatusEnum;
import uk.ac.ebi.subs.data.status.StatusDescription;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;

import java.util.Arrays;
import java.util.List;


@Configuration
public class StatusConfiguration {

    @Bean
    public List<StatusDescription> submissionStatuses() {
        List<StatusDescription> statuses = Arrays.asList(
                StatusDescription.build(SubmissionStatusEnum.Draft, "In preparation")
                        .addUserTransition(SubmissionStatusEnum.Submitted),

                StatusDescription.build(SubmissionStatusEnum.Submitted, "User has submitted documents for storage by archives")
                        .addSystemTransition(SubmissionStatusEnum.Processing),

                StatusDescription.build(SubmissionStatusEnum.Processing, "Submission system is processing the submission")
                        .addSystemTransition(SubmissionStatusEnum.Done),

                StatusDescription.build(SubmissionStatusEnum.Done, "Submission has been stored in the archives")
        );

        return statuses;
    }


    @Bean
    public List<StatusDescription> releaseStatuses() {
        List<StatusDescription> statuses = Arrays.asList(
                StatusDescription.build(ReleaseStatusEnum.Draft, "In preparation")
                        .addUserTransition(ReleaseStatusEnum.Private),

                StatusDescription.build(ReleaseStatusEnum.Private, "Stored in an archive but not released yet")
                        .addUserTransition(ReleaseStatusEnum.Cancelled)
                        .addSystemTransition(ReleaseStatusEnum.Public),

                StatusDescription.build(ReleaseStatusEnum.Cancelled, "Exists in an archive but will not be released")
                        .addUserTransition(ReleaseStatusEnum.Private),

                StatusDescription.build(ReleaseStatusEnum.Public, "Available through an archive")
                        .addSystemTransition(ReleaseStatusEnum.Suppressed)
                        .addSystemTransition(ReleaseStatusEnum.Killed),

                StatusDescription.build(ReleaseStatusEnum.Suppressed, "Data available but not findable without the accession")
                        .addSystemTransition(ReleaseStatusEnum.Public),

                StatusDescription.build(ReleaseStatusEnum.Killed, "Data not available and metadata not findable without the accession")

        );

        return statuses;
    }

    @Bean
    public List<StatusDescription> processingStatuses() {
        List<StatusDescription> statuses = Arrays.asList(

                StatusDescription.build(ProcessingStatusEnum.Draft, "In preparation")
                        .addUserTransition(ProcessingStatusEnum.Submitted),

                StatusDescription.build(ProcessingStatusEnum.Submitted, "User has submitted document for storage by archives")
                        .addSystemTransition(ProcessingStatusEnum.Dispatched),

                StatusDescription.build(ProcessingStatusEnum.Dispatched, "Submission system has dispatched document to the archive")
                        .addSystemTransition(ProcessingStatusEnum.Received),

                StatusDescription.build(ProcessingStatusEnum.Received, "Archive has received document")
                        .addSystemTransition(ProcessingStatusEnum.Curation)
                        .addSystemTransition(ProcessingStatusEnum.Processing),

                StatusDescription.build(ProcessingStatusEnum.Curation, "Curation team is reviewing document")
                        .addSystemTransition(ProcessingStatusEnum.Accepted)
                        .addSystemTransition(ProcessingStatusEnum.ActionRequired),

                StatusDescription.build(ProcessingStatusEnum.Accepted, "Curation team has accepted document")
                        .addSystemTransition(ProcessingStatusEnum.Processing),

                StatusDescription.build(ProcessingStatusEnum.ActionRequired, "Curation team have requested changes or additional information")
                        .addUserTransition(ProcessingStatusEnum.Submitted),

                StatusDescription.build(ProcessingStatusEnum.Processing, "Archive is processing document")
                        .addSystemTransition(ProcessingStatusEnum.Done),

                StatusDescription.build(ProcessingStatusEnum.Done, "Archive has stored document")
        );


        return statuses;
    }

}
