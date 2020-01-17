package uk.ac.ebi.subs.api.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.api.ApiIntegrationTestHelper;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.api.aap.TeamCreationService;
import uk.ac.ebi.subs.api.converters.SubmissionDTOConverter;
import uk.ac.ebi.subs.api.processors.TeamResourceProcessor;
import uk.ac.ebi.subs.api.services.UserTeamService;
import uk.ac.ebi.subs.api.services.UserTokenService;
import uk.ac.ebi.subs.api.validators.TeamDtoValidator;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileRepositoryRest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(TeamController.class)
@MockBeans({
        @MockBean(TeamCreationService.class),
        @MockBean(TeamResourceProcessor.class),
        @MockBean(TeamDtoValidator.class),
        @MockBean(UserTokenService.class),
        @MockBean(SubmissionDTOConverter.class)
})
@EnableSpringDataWebSupport
@WithMockUser(username = "usi_admin_user", roles = {Helpers.TEAM_NAME})
public class TeamControllerTest {

    @MockBean
    private DomainService domainService;
    @MockBean
    private ProfileRepositoryRest profileRepositoryRest;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private UserTeamService userTeamService;

    private final String fakeToken = "TOKEN";
    private final String fakeAuthHeader = "Bearer "+fakeToken;

    @Before
    public void setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(context)
                .defaultRequest(RestDocumentationRequestBuilders.get("/").contextPath("/api"))
                .build();

        ApiIntegrationTestHelper.mockAapProfileAndDomain(domainService, profileRepositoryRest);
    }

    @Test
    public void given5Teams_queryingTeamShouldReturnsExactly5Teams() throws Exception {
        given(userTeamService.userTeams(any())).willReturn(generateAListOfTeams(5));

        mvc.perform(get("/api/user/teams?page=0&size=10")
                .header("Authorization", fakeAuthHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.teams").isArray())
                .andExpect(jsonPath("$._embedded.teams.length()").value(5));
    }

    @Test
    public void given15Teams_queryingTeamsWith1stPageAndSize10ParametersShouldReturnsExactly10Teams() throws Exception {
        given(userTeamService.userTeams(any())).willReturn(generateAListOfTeams(15));

        mvc.perform(get("/api/user/teams?page=0&size=10")
                .header("Authorization", "some value")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.teams").isArray())
                .andExpect(jsonPath("$._embedded.teams.length()").value(10));
    }

    @Test
    public void given15Teams_queryingTeamsWith2ndPageAndSize10ParametersShouldReturnsExactly5Teams() throws Exception {
        given(userTeamService.userTeams(any())).willReturn(generateAListOfTeams(15));

        mvc.perform(get("/api/user/teams?page=1&size=10")
                .header("Authorization", "some value")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.teams").isArray())
                .andExpect(jsonPath("$._embedded.teams.length()").value(5));
    }

    private List<Team> generateAListOfTeams(int numberOfTeams) {
        List<Team> teams = new ArrayList<>(numberOfTeams);
        for (int i = 0; i < numberOfTeams; i++) {
            teams.add(generateTeam());
        }

        return teams;
    }

    private Team generateTeam() {
        Team team = new Team();
        team.setName("Team " + UUID.randomUUID().toString());
        team.setDescription(team.getName());

        return team;
    }
}
