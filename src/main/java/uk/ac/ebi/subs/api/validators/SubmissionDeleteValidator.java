package uk.ac.ebi.subs.api.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.Submission;

/**
 * This class implements a Spring {@link Validator}.
 * It validates the {@link Submission} entity if the given submission could be deleted.
 * The validation executes before the submission deletion.
 * If there is a validation error, then the submission is not getting deleted.
 */
@Component
public class SubmissionDeleteValidator implements Validator {

    @Autowired
    public SubmissionDeleteValidator(OperationControlService operationControlService) {
        this.operationControlService = operationControlService;
    }

    private final Logger logger = LoggerFactory.getLogger(SubmissionDeleteValidator.class);

    private OperationControlService operationControlService;

    @Override
    public boolean supports(Class<?> clazz) {
        return Submission.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Submission submission = (Submission) target;

        if (!operationControlService.isUpdateable(submission)) {
            SubsApiErrors.resource_locked.addError(errors);
        }
    }
}
