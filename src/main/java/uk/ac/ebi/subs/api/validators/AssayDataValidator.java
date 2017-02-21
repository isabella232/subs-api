package uk.ac.ebi.subs.api.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.AssayData;
import uk.ac.ebi.subs.repository.repos.submittables.AssayDataRepository;

@Component
public class AssayDataValidator implements Validator {

    @Autowired
    private CoreSubmittableValidationHelper coreSubmittableValidationHelper;
    @Autowired
    private AssayDataRepository repository;


    @Override
    public boolean supports(Class<?> clazz) {
        return AssayData.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object object, Errors errors) {
        AssayData target = (AssayData) object;
        coreSubmittableValidationHelper.validate(target, repository, errors);
    }
}
