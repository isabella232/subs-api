package uk.ac.ebi.subs.api.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.Protocol;
import uk.ac.ebi.subs.repository.repos.submittables.ProtocolRepository;

@Component
public class ProtocolValidator implements Validator {

    @Autowired
    private CoreSubmittableValidationHelper coreSubmittableValidationHelper;
    @Autowired
    private ProtocolRepository repository;


    @Override
    public boolean supports(Class<?> clazz) {
        return Protocol.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object object, Errors errors) {
        Protocol target = (Protocol) object;
        coreSubmittableValidationHelper.validate(target, repository, errors);
    }
}