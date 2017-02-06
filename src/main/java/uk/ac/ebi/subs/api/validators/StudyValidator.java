package uk.ac.ebi.subs.api.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.Study;
import uk.ac.ebi.subs.repository.repos.StudyRepository;

@Component
public class StudyValidator implements Validator {

    @Autowired
    private CoreSubmittableValidationHelper coreSubmittableValidationHelper;
    @Autowired
    private StudyRepository studyRepository;


    @Override
    public boolean supports(Class<?> clazz) {
        return Study.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Study study = (Study) target;
        coreSubmittableValidationHelper.validate(study, studyRepository, errors);
    }
}

