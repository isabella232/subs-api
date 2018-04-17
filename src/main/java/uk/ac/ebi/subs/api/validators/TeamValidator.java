package uk.ac.ebi.subs.api.validators;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.data.component.Team;

@Component
public class TeamValidator implements Validator {

    @Value("${usi.teamName.prefix}")
    @Getter
    private String expectedTeamNamePrefix;

    private static final String[] expectedProfileAttributes = new String[]{"centre name"};


    @Override
    public boolean supports(Class<?> clazz) {
        return Team.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Team team = (Team) target;
        SubsApiErrors.rejectIfEmptyOrWhitespace(errors, "name");

        if (team.getName() == null || team.getName().isEmpty()) {
            return;
        }

        if (!team.getName().startsWith(expectedTeamNamePrefix)) {
            SubsApiErrors.not_a_subs_team.addError(errors, "name");
        }

        for (String expectedProfileAttributeKey : expectedProfileAttributes) {
            if (
                    !team.getProfile().containsKey(expectedProfileAttributeKey) ||
                            team.getProfile().get(expectedProfileAttributeKey).isEmpty()) {
                SubsApiErrors.missing_profile_attribute.addError(
                        errors,
                        "profile['" + expectedProfileAttributeKey + "']"
                );
            }
        }
    }
}