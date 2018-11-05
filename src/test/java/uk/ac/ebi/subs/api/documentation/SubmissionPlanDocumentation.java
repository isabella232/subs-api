package uk.ac.ebi.subs.api.documentation;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.DocumentationProducer;
import uk.ac.ebi.subs.api.ApiIntegrationTestHelper;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.SubmissionPlan;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionPlanRepository;

import java.util.stream.Collectors;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@Category(DocumentationProducer.class)
@WithMockUser(username = "usi_admin_user", roles = {Helpers.TEAM_NAME, Helpers.ADMIN_TEAM_NAME})
public class SubmissionPlanDocumentation {

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
    @Value("${usi.docs.hostname:localhost}")
    private String host;
    @Value("${usi.docs.port:8080}")
    private int port;
    @Value("${usi.docs.scheme:http}")
    private String scheme;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private SubmissionPlanRepository submissionPlanRepository;

    @Before
    public void setUp() {
        clearDatabase();
        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);

        ApiIntegrationTestHelper.initialiseDataTypes(dataTypeRepository);



        SubmissionPlan submissionPlan = new SubmissionPlan();
        submissionPlan.setId("examplePlan");
        submissionPlan.setDescription("A plan for submitting sequencing data and supporting information");
        submissionPlan.setDisplayName("sequencing data");
        submissionPlan.setDataTypeIds(
                dataTypeRepository.findAll()
                        .stream()
                        .map(DataType::getId)
                        .collect(Collectors.toList())
        );

        submissionPlanRepository.save(submissionPlan);

    }

    @After
    public void clearDatabase() {
        submissionPlanRepository.deleteAll();
        dataTypeRepository.deleteAll();
    }

    @Test
    public void list_submission_plans() throws Exception {

        this.mockMvc.perform(get("/api/submissionPlans"))
                .andExpect(status().isOk())
                .andDo(document(
                        "list-submission-plans",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        links(
                                halLinks(),
                                linkWithRel("profile").ignored(),
                                linkWithRel("self").ignored()
                        ),
                        responseFields(
                                fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources"),
                                fieldWithPath("_embedded.submissionPlans").description("page of submission plan descriptions"),
                                fieldWithPath("page").ignored()
                        )
                ));

    }

    @Test
    public void get_one_submission_plan() throws Exception {
        SubmissionPlan submissionPlan= submissionPlanRepository.findAll().iterator().next();

        this.mockMvc.perform(get("/api/submissionPlans/{id}", submissionPlan.getId()))
                .andExpect(status().isOk())
                .andDo(document(
                        "get-submission-plan",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        links(
                                halLinks(),
                                linkWithRel("self").ignored(),
                                linkWithRel("submissionPlan").ignored()
                        ),
                        responseFields(
                                fieldWithPath("id").ignored(),
                                fieldWithPath("displayName").description("display name for the submission plan"),
                                fieldWithPath("description").description("description of the submission plan"),
                                fieldWithPath("dataTypeIds").description("IDs for the data types used in this plan"),
                                fieldWithPath("dataTypes").description("The data types used in this plan"),
                                fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources")

                        )
                ));

    }
}


