package uk.ac.ebi.subs.api;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileRepositoryRest;
import uk.ac.ebi.tsc.aap.client.security.UserAuthentication;
import uk.ac.ebi.tsc.aap.client.security.WithMockAAPUser;
import uk.ac.ebi.tsc.aap.client.security.WithMockAAPUserSecurityContextFactory;

import java.util.Arrays;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
public class SubmissionAuditTest {

    public static final String DEFAULT_USER_REFERENCE = "submission_audit_test_usr-12345";
    public static final String USI_USER = "submission_audit_test_usi_user";
    public static final String USI_USER_EMAIL = "submission_audit_test_user@usi.org";
    public static final String USER_FULL_NAME = "submission_audit_test_full_name";
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

        ApiIntegrationTestHelper.mockAapProfileAndDomain(domainService, profileRepositoryRest);
    }

    @After
    public void finish() {
        submissionRepository.deleteAll();
    }

    @Test
    public void postSubmissionAndCheckAAPAuditInfo() throws Exception {
        createAapSecurityContext();
        final Submission submission = postSubmission();
        Assert.assertEquals(DEFAULT_USER_REFERENCE, submission.getCreatedBy());
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

    private void createAapSecurityContext(){
        User user = User.builder()
                .withReference(DEFAULT_USER_REFERENCE)
                .withUsername(USI_USER)
                .withEmail(USI_USER_EMAIL)
                .withFullName(USER_FULL_NAME)
                .withDomains(Helpers.TEAM_NAME)
                .build();


        SecurityContext sc = WithMockAAPUserSecurityContextFactory.setUserInSecurityContext(user);
    }

}
