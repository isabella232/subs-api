package uk.ac.ebi.subs.api.validators;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.Errors;
import uk.ac.ebi.subs.data.component.Team;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TeamValidator.class)
public class TeamValidatorTest {

    @Autowired
    private TeamValidator teamValidator;

    @MockBean
    private Errors errors;

    @Test
    public void team_with_no_name_fails(){
        Team team = Team.build(null);

        teamValidator.validate(team,errors);

        verify(errors).rejectValue("name",SubsApiErrors.missing_field.name(),null,SubsApiErrors.missing_field.name());
    }

    @Test
    public void team_with_empty_name_fails(){
        Team team = Team.build("");

        teamValidator.validate(team,errors);

        verify(errors).rejectValue("name",SubsApiErrors.missing_field.name(),null,SubsApiErrors.missing_field.name());

    }

    @Test
    public void team_with_bad_name_fails(){
        Team team = Team.build("nargles");

        teamValidator.validate(team,errors);

        verify(errors).rejectValue("name",SubsApiErrors.not_a_subs_team.name(),SubsApiErrors.not_a_subs_team.name());

    }

    @Test
    public void team_with_good_name_passes(){
        String name = teamValidator.getExpectedTeamNamePrefix() + "1234";

        Team team = Team.build(name);
        team.getProfile().put("centre name","An Institute");
        teamValidator.validate(team,errors);

        verify(errors, never()).rejectValue(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
        );

        verify(errors, never()).reject(
                Mockito.anyString(),
                Mockito.anyString()
        );

    }
}
