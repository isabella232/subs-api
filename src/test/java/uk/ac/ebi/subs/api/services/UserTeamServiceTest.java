package uk.ac.ebi.subs.api.services;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.tsc.aap.client.exception.AAPException;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.Profile;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class UserTeamServiceTest {

    private UserTeamService userTeamService;

    @MockBean
    private DomainService domainService;

    @MockBean
    private ProfileService profileService;

    private static final String USER_TOKEN = "JWT.ABC.123";

    @Before
    public void setup() {
        userTeamService = new UserTeamService(domainService, profileService);
        userTeamService.setTeamNamePrefix("subs.");
    }

    @Test
    public void whenUserTokenIsOutdated_ThenItWontShowTheFreslyAddedDomains() {
        when(domainService.getMyDomains(USER_TOKEN)).thenReturn(generateDomains());
        when(profileService.getDomainProfile(anyString(), anyString())).thenAnswer(
            invocation -> {
                Object argument = invocation.getArguments()[0];
                if (argument.equals("TestDomain_4_reference")) {
                    throw new AAPException("message");
                } else {
                    return generateProfile();
                }
            }
        );

        List<Team> teams = userTeamService.userTeams(USER_TOKEN);

        assertThat(teams, not(IsEmptyCollection.empty()));
        assertThat(teams.size(), is(4));
        assertThat(teams, not(hasItem(Team.build("subs.4"))));
    }

    private Collection<Domain> generateDomains() {
        Collection<Domain> domains = new HashSet<>();

        for (int i = 0; i < 5; i++) {
            Domain domain = getDomain(i);
            domains.add(domain);
        }

        return domains;
    }

    private Domain getDomain(int domainId) {
        return new Domain("subs." + domainId, "TestDomain_" + domainId + "_description", "TestDomain_" + domainId + "_reference");
    }

    private Profile generateProfile() {
        User user = new User("userName", "email", "userReference", "fullName", Collections.emptySet());
        return new Profile("reference", user, getDomain(1), Collections.emptyMap());
    }
}
