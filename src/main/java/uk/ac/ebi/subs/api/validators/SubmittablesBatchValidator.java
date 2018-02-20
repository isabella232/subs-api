package uk.ac.ebi.subs.api.validators;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.SubmittablesBatch;
import uk.ac.ebi.subs.repository.repos.SubmittablesBatchRepository;

@Component
@RequiredArgsConstructor
public class SubmittablesBatchValidator implements Validator {

    @NonNull
    private SubmittablesBatchRepository submittablesBatchRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return SubmittablesBatch.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        SubmittablesBatch batch = (SubmittablesBatch)target;

        if (batch.getId() == null || submittablesBatchRepository.findOne(batch.getId()) == null ){
            //create
            if (batch.getSubmission() == null){
                SubsApiErrors.missing_field.addError(errors,"submission");
            }
            if (batch.getDocuments() == null){
                SubsApiErrors.missing_field.addError(errors,"documents");
            }
        }
        else {
            //update
            SubsApiErrors.resource_locked.addError(errors);
        }

    }
}
