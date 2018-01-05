package uk.ac.ebi.subs.api.validators;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;

@Component
public class SheetValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return Sheet.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        //TODO in SUBS-1036
    }
}
