package uk.ac.ebi.subs.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.status.ReleaseStatusEnum;
import uk.ac.ebi.subs.data.status.StatusDescription;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sets the transitions for the various submission statuses: submission status, release status, processing status.
 * A transition could be done by a user or the system.
 * It can be configured in this configuration file, but this is only a kind of documentation.
 * This configuration is not used to enforced the different possible transitions.
 * The user transitions are validated with validators.
 */
@Configuration
public class StatusConfiguration {

    @Bean
    public Map<String, StatusDescription> submissionStatusDescriptionMap() {
        return statusListToMapKeyedOnName(submissionStatuses());
    }

    @Bean
    public Map<String, StatusDescription> processingStatusDescriptionMap() {
        return statusListToMapKeyedOnName(processingStatuses());
    }

    @Bean
    public Map<String, StatusDescription> releaseStatusDescriptionMap() {
        return statusListToMapKeyedOnName(releaseStatuses());
    }

    @Bean
    public List<StatusDescription> submissionStatuses() {
        List<StatusDescription> statuses = Arrays.asList(
                StatusDescription.build(SubmissionStatusEnum.Draft, "In preparation")
                        .addUserTransition(SubmissionStatusEnum.Submitted)
                        .acceptUpdates(),

                StatusDescription.build(SubmissionStatusEnum.Submitted, "User has submitted documents for storage by archives")
                        .addSystemTransition(SubmissionStatusEnum.Processing),

                StatusDescription.build(SubmissionStatusEnum.Processing, "Submission system is processing the submission")
                        .addSystemTransition(SubmissionStatusEnum.Completed),

                StatusDescription.build(SubmissionStatusEnum.Completed, "Submission has been stored in the archives")
        );

        return Collections.unmodifiableList(statuses);
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
                        .addUserTransition(ProcessingStatusEnum.Submitted)
                        .acceptUpdates(),

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
                        .addUserTransition(ProcessingStatusEnum.Submitted)
                        .acceptUpdates(),

                StatusDescription.build(ProcessingStatusEnum.Processing, "Archive is processing document")
                        .addSystemTransition(ProcessingStatusEnum.Completed),

                StatusDescription.build(ProcessingStatusEnum.Completed, "Archive has stored document"),

                StatusDescription.build(ProcessingStatusEnum.Error, "Archive agent has rejected a document"),

                StatusDescription.build(ProcessingStatusEnum.Rejected, "Archive agent has rejected a document"),

                StatusDescription.build(ProcessingStatusEnum.ArchiveDisabled, "Dispatching submission has been disabled to this archive")
        );


        return statuses;
    }

    private Map<String, StatusDescription> statusListToMapKeyedOnName(List<StatusDescription> statusDescriptions) {
        final Map<String, StatusDescription> stringStatusDescriptionMap = new HashMap<>(statusDescriptions.size());

        statusDescriptions.forEach(sd -> stringStatusDescriptionMap.put(sd.getStatusName(), sd));

        return Collections.unmodifiableMap(stringStatusDescriptionMap);
    }

}
