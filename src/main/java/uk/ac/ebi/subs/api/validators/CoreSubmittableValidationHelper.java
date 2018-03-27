package uk.ac.ebi.subs.api.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.submittable.Submittable;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Base validator for submitted items
 * <p>
 * Ensures that we have a submission ID and that it relates to a real submission
 * <p>
 * Note that we must supply a default message. Not having a message causes the client to get a 500 (server error)
 * status code instead of a 400 (bad request)
 */
@Component
public class CoreSubmittableValidationHelper {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private OperationControlService operationControlService;

    @Autowired
    public CoreSubmittableValidationHelper(OperationControlService operationControlService) {
        this.operationControlService = operationControlService;
    }

    public void validate(StoredSubmittable target, SubmittableRepository repository, Errors errors) {
        logger.debug("validating {}", target);
        StoredSubmittable storedVersion = null;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "submission", "required", "submission is required");

        if (errors.hasErrors()) {
            return;
        }

        if (target.getId() != null) {
            storedVersion = repository.findOne(target.getId());
        }

        this.validateAlias(target,repository,errors);

        this.validate(target, storedVersion, errors);

        this.validateIfDuplicateWithinTeamAsDraft(target, repository, errors);
    }

    public void validateAlias(StoredSubmittable target, SubmittableRepository repository, Errors errors) {

        ValidationUtils.rejectIfEmptyOrWhitespace(errors,"alias","required", "alias is required");

        validateOnlyUseOfAliasInSubmission(target, repository, errors);
    }

    public void validateOnlyUseOfAliasInSubmission(StoredSubmittable target, SubmittableRepository repository, Errors errors) {
        if (target.getAlias() == null || target.getSubmission() == null) {
            return;
        }

        List<? extends StoredSubmittable> itemsInSubmissionWithSameAlias = repository.findBySubmissionIdAndAliasIn(target.getSubmission().getId(), Arrays.asList(target.getAlias()));

        Optional<? extends StoredSubmittable> itemWithSameAliasDifferentId = itemsInSubmissionWithSameAlias.stream()
                .filter(item -> !item.getId().equals(target.getId()))
                .findAny();

        if (itemWithSameAliasDifferentId.isPresent()) {
            SubsApiErrors.already_exists.addError(errors, "alias");
        }
    }

    public void validate(StoredSubmittable target, StoredSubmittable storedVersion, Errors errors) {
        StoredSubmittable submittable = target;

        if (submittable.getSubmission() != null && !operationControlService.isUpdateable(submittable.getSubmission())) {
            SubsApiErrors.resource_locked.addError(errors);
        }

        if (errors.hasErrors()) return;

        if (storedVersion != null && !operationControlService.isUpdateable(storedVersion)) {
            SubsApiErrors.resource_locked.addError(errors);
        }

        if (storedVersion != null) {
            validateAgainstStoredVersion(errors, submittable, storedVersion);
        }
    }

    private void validateAgainstStoredVersion(Errors errors, StoredSubmittable submittable, StoredSubmittable storedVersion) {

        ValidationHelper.thingCannotChange(
                (submittable.getSubmission() == null) ? null : submittable.getSubmission().getId(),
                (storedVersion.getSubmission() == null) ? null : storedVersion.getSubmission().getId(),
                "submission",
                errors
        );

        /* Yes, this is stupid
         * Spring Data Auditing is set for this object, but it doesn't maintain the createdDate on save
         */

        submittable.setCreatedDate(storedVersion.getCreatedDate());
        submittable.setCreatedBy(storedVersion.getCreatedBy());
    }

    public void validateIfDuplicateWithinTeamAsDraft(StoredSubmittable target, SubmittableRepository repository, Errors errors) {
        if (target.getAlias() == null || target.getSubmission() == null || target.getSubmission().getTeam() == null || target.getSubmission().getTeam().getName() == null) {
            return;
        }

        List<StoredSubmittable> results = repository.findByTeamNameAndAliasOrderByCreatedDateDesc(target.getSubmission().getTeam().getName(), target.getAlias(), new PageRequest(0,50)).getContent();

        List<? extends StoredSubmittable> itemsWithSameAliasDifferentId = results.stream()
                .filter(item -> !item.getId().equals(target.getId()))
                .collect(Collectors.toList());

        boolean duplicateItem = itemsWithSameAliasDifferentId.stream()
                .anyMatch(item -> item.getProcessingStatus() == null || !item.getProcessingStatus().getStatus().equals(ProcessingStatusEnum.Completed.name()));

        if (duplicateItem) {
            SubsApiErrors.already_exists_and_not_completed.addError(errors);
        }
    }
}
