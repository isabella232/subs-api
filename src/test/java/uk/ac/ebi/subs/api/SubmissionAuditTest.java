package uk.ac.ebi.subs.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileRepositoryRest;
import uk.ac.ebi.tsc.aap.client.security.WithMockAAPUser;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@ActiveProfiles("aap")
public class SubmissionAuditTest {

    public static final String DEFAULT_USER_REFERENCE = "usr-12345";
    public static final String USI_USER = "usi_user";
    public static final String USI_USER_EMAIL = "usi-user@usi.org";
    public static final String USER_FULL_NAME = "Test User";
    private MockMvc mockMvc;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private DomainService domainService;

    @MockBean
    private ProfileRepositoryRest profileRepositoryRest;

    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .build();

        ApiIntegrationTestHelper.mockAapProfileAndDomain(domainService,profileRepositoryRest);
    }

    @After
    public void finish() {
        submissionRepository.deleteAll();
    }

    @Test
    @WithMockAAPUser(userName = USI_USER, email = USI_USER_EMAIL, userReference = DEFAULT_USER_REFERENCE, fullName = USER_FULL_NAME, domains = {Helpers.TEAM_NAME})
    public void postSubmissionAndCheckAAPAuditInfo() throws Exception {
        final Submission submission = postSubmission();
        Assert.assertEquals(DEFAULT_USER_REFERENCE, submission.getCreatedBy());
    }

    @Test
    @WithMockUser(username = USI_USER, roles = {Helpers.TEAM_NAME})
    public void postSubmissionAndCheckAuditInfo() throws Exception {
        final Submission submission = postSubmission();
        Assert.assertEquals(USI_USER, submission.getCreatedBy());
    }

    private Submission postSubmission() throws Exception {

        String url = "/teams/" + Helpers.TEAM_NAME + "/submissions";

        this.mockMvc.perform(
                post(url).content("{}")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .header("Authorization", "Bearer token")
                        .accept(RestMediaTypes.HAL_JSON)).andExpect(status().isCreated());

        return submissionRepository.findAll().get(0);
    }

}
