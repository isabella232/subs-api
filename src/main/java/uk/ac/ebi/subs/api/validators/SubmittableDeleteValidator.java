package uk.ac.ebi.subs.api.validators;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;

@Component
public class SubmittableDeleteValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return StoredSubmittable.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        StoredSubmittable submittable = (StoredSubmittable) target;


        if (!ProcessingStatusEnum.Draft.name().equals(submittable.getProcessingStatus().getStatus())) {
            SubsApiErrors.resource_locked.addError(errors);
        }

    }
}
