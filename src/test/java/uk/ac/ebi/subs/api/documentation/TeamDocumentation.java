package uk.ac.ebi.subs.api.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.DocumentationProducer;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.api.services.SubmissionEventService;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.AssayDataRepository;
import uk.ac.ebi.subs.repository.repos.submittables.AssayRepository;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;
import uk.ac.ebi.subs.repository.repos.submittables.StudyRepository;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

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
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.addAuthTokenHeader;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@Category(DocumentationProducer.class)
@WithMockUser(username = "usi_user", roles = {Helpers.TEAM_NAME})
public class TeamDocumentation {

    @Rule
    public final JUnitRestDocumentation restDocumentation = DocumentationHelper.jUnitRestDocumentation();

    @Value("${usi.docs.hostname:localhost}")
    private String host;
    @Value("${usi.docs.port:8080}")
    private int port;
    @Value("${usi.docs.scheme:http}")
    private String scheme;


    @Autowired
    private WebApplicationContext context;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);
        this.objectMapper = DocumentationHelper.mapper();
    }

    @Test
    public void team() throws Exception {

        this.mockMvc.perform(
                get("/api/teams/{teamName}", Helpers.TEAM_NAME)
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("get-team",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("submissions").description("Collection of submissions within this team"),
                                        linkWithRel("submissions:create").description("Collection of submissions within this team"),
                                        linkWithRel("items").description("Items owned by this team")
                                ),
                                responseFields(
                                        DocumentationHelper.linksResponseField(),
                                        fieldWithPath("name").description("Name of this team")
                                )
                        )
                );
    }

    @Test
    public void teams() throws Exception {


        this.mockMvc.perform(
                get("/api/user/teams")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("get-teams",
                                preprocessRequest(prettyPrint(),addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        linkWithRel("self").description("This resource list")
                                ),
                                responseFields(
                                        fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources"),
                                        fieldWithPath("_embedded").description("The list of resources"),
                                        fieldWithPath("_embedded.teams[].name").description("Name of this team"),
                                        fieldWithPath("page.size").description("The number of resources in this page"),
                                        fieldWithPath("page.totalElements").description("The total number of resources"),
                                        fieldWithPath("page.totalPages").description("The total number of pages"),
                                        fieldWithPath("page.number").description("The page number")
                                )
                        )
                );
    }
}
