package uk.ac.ebi.subs.api.validators;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.data.status.ProcessingStatus;
import uk.ac.ebi.subs.data.submittable.Submittable;

@Component
public class SubmittableDeleteValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return Submittable.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Submittable submittable = (Submittable) target;

        if (!ProcessingStatus.Draft.name().equals(submittable.getStatus())) {
            errors.reject("cannotDeleteAfterSubmission", "Deletion is not possible after submission");
        }

    }
}
