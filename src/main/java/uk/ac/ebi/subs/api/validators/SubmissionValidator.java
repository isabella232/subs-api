package uk.ac.ebi.subs.api.validators;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.SubmissionRepository;
import uk.ac.ebi.subs.repository.model.Submission;

@Component
public class SubmissionValidator implements Validator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public SubmissionValidator(
            SubmissionRepository submissionRepository,
            DomainValidator domainValidator,
            SubmitterValidator submitterValidator,
            OperationControlService operationControlService
    ) {
        this.submissionRepository = submissionRepository;
        this.domainValidator = domainValidator;
        this.submitterValidator = submitterValidator;
        this.operationControlService = operationControlService;
    }


    private SubmissionRepository submissionRepository;
    private DomainValidator domainValidator;
    private SubmitterValidator submitterValidator;
    private OperationControlService operationControlService;


    @Override
    public void validate(Object target, Errors errors) {

        Submission submission = (Submission) target;

        ValidationUtils.rejectIfEmpty(errors, "submitter", "required", "submitter is required");
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


                if (!operationControlService.isUpdateable(submission)) {
                    errors.reject("submissionLocked", "Submission has been submitted, changes are not possible");
                } else {
                    validateAgainstStoredVersion(submission, storedVersion, errors);
                }
            }
        }

        if (errors.hasErrors()) {
            logger.error("validation has errors {}", errors.getAllErrors());
        } else {
            logger.error("no validation errors");
        }

    }

    private void validateAgainstStoredVersion(Submission target, Submission storedVersion, Errors errors) {

        submitterCannotChange(target, storedVersion, errors);

        domainCannotChange(target, storedVersion, errors);


        createdDateCannotChange(target, storedVersion, errors);

        submittedDateCannotChange(target, storedVersion, errors);
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
