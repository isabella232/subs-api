package uk.ac.ebi.subs.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.api.controllers.SubmissionController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WithMockUser(username = "submission_controller_test_usi_user", roles = {Helpers.TEAM_NAME})
public class SubmissionControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SubmissionController submissionController;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .defaultRequest(get("/").contextPath("/api"))
                .build();
    }

    @Test
    public void contextLoads() {
        assertThat(submissionController).isNotNull();
    }

    @Test
    public void returnsRightHeaders() throws Exception {
        this.mockMvc.perform(options("/api/submissions/123456"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET,DELETE,OPTIONS"));
    }

}
