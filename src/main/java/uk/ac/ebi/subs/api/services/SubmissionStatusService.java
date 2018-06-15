package uk.ac.ebi.subs.api.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.data.status.StatusDescription;
import uk.ac.ebi.subs.repository.model.Submission;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubmissionStatusService {

    @NonNull
    private ValidationResultService validationResultService;
    @NonNull
    private FileService fileService;

    public Collection<String> getAvailableStatusNames(Submission currentSubmission,
                                                      Map<String, StatusDescription> submissionStatusDescriptionMap) {
        Collection<String> statusNames;

        if (validationResultService.isValidationFinishedAndPassed(currentSubmission.getSubmissionStatus().getId())
                && fileService.allFilesBySubmissionIDReadyForArchive(currentSubmission.getId())) {
            StatusDescription statusDescription = submissionStatusDescriptionMap.get(currentSubmission.getSubmissionStatus().getStatus());

            statusNames = statusDescription.getUserTransitions();
        } else {
            statusNames = Collections.emptySet();
        }

        return statusNames;
    }
}
