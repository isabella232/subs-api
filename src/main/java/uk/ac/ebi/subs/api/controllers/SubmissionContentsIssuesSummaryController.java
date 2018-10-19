package uk.ac.ebi.subs.api.controllers;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;
import uk.ac.ebi.subs.validator.repository.ValidatorResultRepositoryCustom;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SubmissionContentsIssuesSummaryController {

    @NonNull
    private FileRepository fileRepository;
    @NonNull
    private ValidatorResultRepositoryCustom validatorResultRepositoryCustom;

    @GetMapping(value = "/submissions/{submissionId}/contents/issuesSummary")
    public SubmissionIssuesSummary getSubmissionContentsIssuesSummary(@PathVariable @P("submissionId") String submissionId) {
        SubmissionIssuesSummary submissionIssuesSummary = new SubmissionIssuesSummary();

        getFileIssues(submissionId, submissionIssuesSummary);
        getMetadataIssues(submissionId, submissionIssuesSummary);

        return submissionIssuesSummary;
    }

    private void getFileIssues(String submissionId, SubmissionIssuesSummary submissionIssuesSummary) {
        List<File> files = fileRepository.findBySubmissionId(submissionId);

        for (File file : files) {
            if (!file.getStatus().equals(FileStatus.READY_FOR_ARCHIVE)) {
                submissionIssuesSummary.notReadyFileCount++;
            }
        }
    }

    private void getMetadataIssues(String submissionId, SubmissionIssuesSummary submissionIssuesSummary) {
        submissionIssuesSummary.setValidationIssuesPerDataTypeId(
                validatorResultRepositoryCustom.validationIssuesPerDataTypeId(submissionId));
    }

    @Data
    class SubmissionIssuesSummary {
        int notReadyFileCount;
        Map<String, Integer> validationIssuesPerDataTypeId;
    }
}
