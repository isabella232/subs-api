package uk.ac.ebi.subs.api.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.Assay;
import uk.ac.ebi.subs.repository.repos.submittables.AssayRepository;

@Component
public class AssayValidator implements Validator {

    @Autowired
    private CoreSubmittableValidationHelper coreSubmittableValidationHelper;
    @Autowired
    private AssayRepository assayRepository;


    @Override
    public boolean supports(Class<?> clazz) {
        return Assay.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Assay assay = (Assay) target;
        coreSubmittableValidationHelper.validate(assay, assayRepository, errors);
    }
}
