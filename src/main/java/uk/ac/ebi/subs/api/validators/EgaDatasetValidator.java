package uk.ac.ebi.subs.api.validators;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.EgaDataset;
import uk.ac.ebi.subs.repository.repos.submittables.EgaDatasetRepository;

/**
 * This class implements a Spring {@link Validator}.
 * It validates the {@link EgaDataset} entity.
 */
@Component
public class EgaDatasetValidator implements Validator {

    @Autowired
    private CoreSubmittableValidationHelper coreSubmittableValidationHelper;
    @Autowired
    private EgaDatasetRepository repository;


    @Override
    public boolean supports(Class<?> clazz) {
        return EgaDataset.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object object, Errors errors) {
        EgaDataset target = (EgaDataset) object;
        coreSubmittableValidationHelper.validate(target, repository, errors);
    }
}

