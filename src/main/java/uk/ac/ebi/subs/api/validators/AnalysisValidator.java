package uk.ac.ebi.subs.api.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.Analysis;
import uk.ac.ebi.subs.repository.repos.submittables.AnalysisRepository;

@Component
public class AnalysisValidator implements Validator {

    @Autowired
    private CoreSubmittableValidationHelper coreSubmittableValidationHelper;
    @Autowired
    private AnalysisRepository analysisRepository;


    @Override
    public boolean supports(Class<?> clazz) {
        return Analysis.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Analysis analysis = (Analysis) target;
        coreSubmittableValidationHelper.validate(analysis, analysisRepository, errors);
    }
}
