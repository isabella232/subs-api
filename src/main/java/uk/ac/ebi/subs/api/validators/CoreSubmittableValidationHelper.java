package uk.ac.ebi.subs.api.validators;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.api.services.UserAuthoritiesService;
import uk.ac.ebi.subs.api.services.UserTeamService;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base validator for submitted items
 * <p>
 * Ensures that we have a submission ID and that it relates to a real submission
 * <p>
 * Note that we must supply a default message. Not having a message causes the client to get a 500 (server error)
 * status code instead of a 400 (bad request)
 */
@Component
@RequiredArgsConstructor
public class CoreSubmittableValidationHelper {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @NonNull
    private OperationControlService operationControlService;

    @NonNull
    private UserAuthoritiesService userAuthoritiesService;

    private static final String DATA_MIGRATION_DOMAIN_NAME = "embl-ebi-subs-data-migrator";




    public void validate(StoredSubmittable target, SubmittableRepository repository, Errors errors) {
        logger.debug("validating {}", target);
        StoredSubmittable storedVersion = null;

        SubsApiErrors.rejectIfEmptyOrWhitespace(errors, "submission");
        SubsApiErrors.rejectIfEmptyOrWhitespace(errors, "dataType");

        ensureChecklistIsForSameDataTypeAsSubmittable(target, errors);

        if (errors.hasErrors()) {
            return;
        }

        if (target.getId() != null) {
            storedVersion = repository.findOne(target.getId());
        }

        this.validateAlias(target, repository, errors);

        this.validate(target, storedVersion, errors);

        this.validateAliasIsLockedToDataType(target, repository, errors);

        this.validateAccessionIsKnown(target, repository, errors);
    }

    /**
     * If an accession is provided, it should already be recorded in the database. If it is known, the team name,
     * alias and data type, must match.
     *
     * If the user is has the data migrator role, they can use an accession, even if USI don't know it
     *
     * @param target
     * @param repository
     * @param errors
     */
    private void validateAccessionIsKnown(StoredSubmittable target, SubmittableRepository repository, Errors errors) {

        if (target.getAccession() == null) {
            return;
        }

        StoredSubmittable accessionedRecord = repository.findFirstByAccessionOrderByCreatedDateDesc(target.getAccession());

        if (accessionedRecord == null) {
            if (!userIsDataMigrator()) {
                SubsApiErrors.unknown_accession.addError(errors, "accession");
            }
            return;
        }

        if (!accessionedRecord.getAlias().equals(target.getAlias())) {
            SubsApiErrors.inconsistent_with_previous_records.addError(errors, "alias");
        }

        if (!accessionedRecord.getTeam().getName().equals(target.getTeam().getName())){
            SubsApiErrors.inconsistent_with_previous_records.addError(errors, "team.name");
        }

        if (!accessionedRecord.getDataType().equals(target.getDataType())){
            SubsApiErrors.inconsistent_with_previous_records.addError(errors, "dataType");
        }
    }

    private boolean userIsDataMigrator(){
        return userAuthoritiesService.userAuthoritiesStream().anyMatch(DATA_MIGRATION_DOMAIN_NAME::equals);
    }

    private void ensureChecklistIsForSameDataTypeAsSubmittable(StoredSubmittable target, Errors errors) {
        DataType dataType = target.getDataType();
        Checklist checklist = target.getChecklist();

        if (dataType != null && checklist != null
                && !dataType.getId().equals(checklist.getDataTypeId())) {
            SubsApiErrors.invalid.addError(errors, "checklist");
        }
    }

    public void validateAlias(StoredSubmittable target, SubmittableRepository repository, Errors errors) {

        SubsApiErrors.rejectIfEmptyOrWhitespace(errors, "alias");
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

        ValidationHelper.thingCannotChange(
                (submittable.getDataType() == null) ? null : submittable.getDataType().getId(),
                (storedVersion.getDataType() == null) ? null : storedVersion.getDataType().getId(),
                "dataType",
                errors
        );

        /* Yes, this is stupid
         * Spring Data Auditing is set for this object, but it doesn't maintain the createdDate on save
         */

        submittable.setCreatedDate(storedVersion.getCreatedDate());
        submittable.setCreatedBy(storedVersion.getCreatedBy());
    }


    public void validateAliasIsLockedToDataType(StoredSubmittable target, SubmittableRepository repository, Errors errors) {
        if (target.getAlias() == null) {
            return;
        }

        Stream<StoredSubmittable> itemsWithAliasStream = repository.streamByTeamNameAndAliasOrderByCreatedDateDesc(
                target.getSubmission().getTeam().getName(),
                target.getAlias());

        Set<String> dataTypeIds = itemsWithAliasStream
                .map(doc -> doc.getDataType())
                .filter(Objects::nonNull)
                .map(dataType -> dataType.getId())
                .distinct()
                .collect(Collectors.toSet());


        if (dataTypeIds.size() > 1) {
            throw new IllegalStateException("Multiple dataTypes found in history of item " + target);
        }

        if (dataTypeIds.size() == 1 && !target.getDataType().getId().equals(dataTypeIds.iterator().next())) {
            SubsApiErrors.invalid.addError(errors, "dataType");
        }
    }
}
