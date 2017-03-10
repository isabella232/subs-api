package uk.ac.ebi.subs.api.validators;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.data.component.Team;

@Component
public class TeamValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return Team.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Team team = (Team) target;
        SubsApiErrors.rejectIfEmptyOrWhitespace(errors,"name");
    }
}
