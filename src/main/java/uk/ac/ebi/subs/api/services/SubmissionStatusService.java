package uk.ac.ebi.subs.api.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.data.status.StatusDescription;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * This is a Spring @Service component for {@link SubmissionStatus} entity.
 */
@Service
@RequiredArgsConstructor
public class SubmissionStatusService {

    @NonNull
    private ValidationResultService validationResultService;
    @NonNull
    private FileService fileService;
    @NonNull
    private SubmissionRepository submissionRepository;

    public Collection<String> getAvailableStatusNames(Submission currentSubmission,
                                                      Map<String, StatusDescription> submissionStatusDescriptionMap) {
        Collection<String> statusNames;

        if (this.isSubmissionStatusChangeable(currentSubmission)) {
            StatusDescription statusDescription = submissionStatusDescriptionMap.get(currentSubmission.getSubmissionStatus().getStatus());

            statusNames = statusDescription.getUserTransitions();
        } else {
            statusNames = Collections.emptySet();
        }

        return statusNames;
    }

    public boolean isSubmissionStatusChangeable(Submission currentSubmission){
        return validationResultService.isValidationFinishedAndPassed(currentSubmission.getSubmissionStatus().getId())
                && fileService.allFilesBySubmissionIDReadyForArchive(currentSubmission.getId());
    }

    public boolean isSubmissionStatusChangeable(SubmissionStatus submissionStatus){
        Submission submission = submissionRepository.findBySubmissionStatusId(submissionStatus.getId());
        return this.isSubmissionStatusChangeable(submission);
    }
}
