package uk.ac.ebi.subs.api.validators;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;

/**
 * This class implements a Spring {@link Validator}.
 * It validates the {@link StoredSubmittable} entity if the given stored submittable could be deleted.
 * The validation executes before the deletion operation.
 * If there is a validation error, then the stored submittable is not getting deleted.
 */
@Component
public class SubmittableDeleteValidator implements Validator {

    private OperationControlService operationControlService;

    public SubmittableDeleteValidator(OperationControlService operationControlService) {
        this.operationControlService = operationControlService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return StoredSubmittable.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        StoredSubmittable submittable = (StoredSubmittable) target;

        if (!operationControlService.isUpdateable(submittable)) {
            SubsApiErrors.resource_locked.addError(errors);
        }
        //TODO could also lock out deletion of objects that are referenced, but need to consider the rules carefully
    }
}
