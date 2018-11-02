package uk.ac.ebi.subs.api.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.data.structures.GlobalValidationStatus;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;
import uk.ac.ebi.subs.validator.repository.ValidatorResultRepositoryCustom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * REST endpoint that gathers information about the issues, that blocks submitting a submission.
 * This could be the following:
 * - empty submission (no metadata added to the given submission)
 * - validation of a submittable or file object is in progress
 * - file(s) upload is in progress
 * - metadata or file validation has errors
 *
 */
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
    @NonNull
    private DataTypeRepository dataTypeRepository;

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
        Map<String, Integer> validationIssuesPerDataTypeId =
                validatorResultRepositoryCustom.validationIssuesPerDataTypeId(submissionId);
        Map<String, Map<String, Object>> validationBlockersByDataTypeId = new HashMap<>();

        validationIssuesPerDataTypeId.forEach((dataTypeId, count) -> {
            DataType dataType = dataTypeRepository.findOne(dataTypeId);

            if (dataType == null) {
                throw new ResourceNotFoundException();
            }

            String dataTypeDisplayName = dataType.getDisplayNamePlural();

            Map<String, Object> blockerDataTypeProperties = new HashMap<>();
            blockerDataTypeProperties.put("displayName", dataTypeDisplayName);
            blockerDataTypeProperties.put("count", count);

            validationBlockersByDataTypeId.put(dataTypeId, blockerDataTypeProperties);

            submissionBlockersSummary.totalMetadataBlockers += count;
        });

        submissionBlockersSummary.setValidationIssuesPerDataTypeId(validationBlockersByDataTypeId);
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
        Map<String, Map<String, Object>> validationIssuesPerDataTypeId;
        long totalMetadataBlockers;
        boolean emptySubmission;
        boolean anyPendingValidationResult;
    }
}
