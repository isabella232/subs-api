package uk.ac.ebi.subs.api.validators;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.aap.TeamDto;

/**
 * This class implements a Spring {@link Validator}.
 * It validates the {@link TeamDto} entity.
 */
@Component
public class TeamDtoValidator implements Validator{

    @Override
    public boolean supports(Class<?> clazz) {
        return TeamDto.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        SubsApiErrors.rejectIfEmptyOrWhitespace(errors,"centreName");
    }
}
