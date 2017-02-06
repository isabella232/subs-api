package uk.ac.ebi.subs.api.validators;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.status.Status;
import uk.ac.ebi.subs.data.status.SubmissionStatus;
import uk.ac.ebi.subs.repository.SubmissionRepository;

import java.util.List;

@Component
public class SubmissionValidator implements Validator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private DomainValidator domainValidator;

    @Autowired
    private SubmitterValidator submitterValidator;

    @Autowired
    private List<Status> submissionStatuses;

    @Override
    public void validate(Object target, Errors errors) {

        Submission submission = (Submission) target;

        ValidationUtils.rejectIfEmpty(errors, "submitter", "required", "submitter is required");
        ValidationUtils.rejectIfEmpty(errors, "status", "required", "status is required");
        ValidationUtils.rejectIfEmpty(errors, "domain", "required", "domain is required");

        try {
            errors.pushNestedPath("domain");
            ValidationUtils.invokeValidator(this.domainValidator, submission.getDomain(), errors);
        } finally {
            errors.popNestedPath();
        }

        try {
            errors.pushNestedPath("submitter");
            ValidationUtils.invokeValidator(this.submitterValidator, submission.getSubmitter(), errors);
        } finally {
            errors.popNestedPath();
        }

        if (submission.getId() != null) {
            Submission storedVersion = submissionRepository.findOne(submission.getId());

            if (storedVersion != null) {
                if (!storedVersion.getStatus().equals(SubmissionStatus.Draft.name())) {
                    errors.reject("submissionLocked", "Submission has been submitted, changes are not possible");
                } else {
                    validateAgainstStoredVersion(submission, storedVersion, errors);
                }
            }
        }

        if (errors.hasErrors()){
            logger.error("validation has errors {}",errors.getAllErrors());
        }
        else {
            logger.error("no validation errors");
        }

    }

    private void validateAgainstStoredVersion(Submission target, Submission storedVersion, Errors errors) {

        submitterCannotChange(target, storedVersion, errors);

        domainCannotChange(target, storedVersion, errors);

        statusChangeMustBePermitted(target, storedVersion, errors);

        createdDateCannotChange(target, storedVersion, errors);

        submittedDateCannotChange(target, storedVersion, errors);
    }

    private void statusChangeMustBePermitted(Submission target, Submission storedVersion, Errors errors) {
        ValidationHelper.validateStatusChange(
                target.getStatus(),
                storedVersion.getStatus(),
                submissionStatuses,
                "status",
                errors
        );
    }

    private void submitterCannotChange(Submission target, Submission storedVersion, Errors errors) {
        ValidationHelper.thingCannotChange(
                target.getSubmitter(),
                storedVersion.getSubmitter(),
                "submitter",
                errors
        );
    }

    private void domainCannotChange(Submission target, Submission storedVersion, Errors errors) {
        ValidationHelper.thingCannotChange(
                target.getDomain(),
                storedVersion.getDomain(),
                "domain",
                errors
        );
    }

    private void createdDateCannotChange(Submission target, Submission storedVersion, Errors errors) {
        /*Yes, this is stupid
         * Spring Data Auditing is set for this object, but it doesn't maintain the createdDate on save
         */

        target.setCreatedDate(storedVersion.getCreatedDate());
    }

    private void submittedDateCannotChange(Submission target, Submission storedVersion, Errors errors) {
        ValidationHelper.thingCannotChange(
                target.getSubmissionDate(),
                storedVersion.getSubmissionDate(),
                "submissionDate",
                errors
        );
    }


    @Override
    public boolean supports(Class<?> clazz) {
        return Submission.class.isAssignableFrom(clazz);
    }
}
