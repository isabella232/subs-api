package uk.ac.ebi.subs.api.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.data.structures.GlobalValidationStatus;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;
import uk.ac.ebi.subs.validator.repository.ValidatorResultRepositoryCustom;

import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequiredArgsConstructor
public class SubmissionBlockersSummaryController {

    @NonNull
    private FileRepository fileRepository;
    @NonNull
    private ValidatorResultRepositoryCustom validatorResultRepositoryCustom;
    @NonNull
    private ValidationResultRepository validationResultRepository;
    @NonNull
    private ProcessingStatusRepository processingStatusRepository;

    @GetMapping(value = "/submissions/{submissionId}/submissionBlockersSummary")
    public Resource<SubmissionBlockersSummary> getSubmissionContentsIssuesSummary(@PathVariable @P("submissionId") String submissionId) {
        SubmissionBlockersSummary submissionBlockersSummary = new SubmissionBlockersSummary();

        getFileIssues(submissionId, submissionBlockersSummary);
        getMetadataIssues(submissionId, submissionBlockersSummary);
        checkSubmissionEmptiness(submissionId, submissionBlockersSummary);
        checkPendingValidationResults(submissionId, submissionBlockersSummary);

        Resource<SubmissionBlockersSummary> submissionIssuesSummaryResource = new Resource<>(submissionBlockersSummary);

        Link selfLink = linkTo(methodOn(
                this.getClass()).getSubmissionContentsIssuesSummary(submissionId)
        ).withSelfRel();

        submissionIssuesSummaryResource.add(selfLink);

        return submissionIssuesSummaryResource;
    }



    private void getFileIssues(String submissionId, SubmissionBlockersSummary submissionBlockersSummary) {
        submissionBlockersSummary.notReadyFileCount = fileRepository.countBySubmissionIdAndStatusNot(submissionId, FileStatus.READY_FOR_ARCHIVE);
    }

    private void getMetadataIssues(String submissionId, SubmissionBlockersSummary submissionBlockersSummary) {
        submissionBlockersSummary.setValidationIssuesPerDataTypeId(
                validatorResultRepositoryCustom.validationIssuesPerDataTypeId(submissionId));
    }

    private void checkSubmissionEmptiness(String submissionId, SubmissionBlockersSummary submissionBlockersSummary) {
        List<ValidationResult> validationResults = validationResultRepository.findAllBySubmissionId(submissionId);
        List<ProcessingStatus> processingStatuses = processingStatusRepository.findBySubmissionId(submissionId);

        if (validationResults.size() == 0 || processingStatuses.size() == 0) {
            submissionBlockersSummary.setEmptySubmission(true);
        }
    }

    private void checkPendingValidationResults(String submissionId, SubmissionBlockersSummary submissionBlockersSummary) {
        submissionBlockersSummary.anyPendingValidationResult =
                validationResultRepository.countBySubmissionIdAndValidationStatusIs(submissionId, GlobalValidationStatus.Pending) > 0;
    }

    @Data
    class SubmissionBlockersSummary {
        long notReadyFileCount;
        @JsonInclude(content= JsonInclude.Include.NON_EMPTY)
        Map<String, Integer> validationIssuesPerDataTypeId;
        boolean emptySubmission;
        boolean anyPendingValidationResult;
    }
}
