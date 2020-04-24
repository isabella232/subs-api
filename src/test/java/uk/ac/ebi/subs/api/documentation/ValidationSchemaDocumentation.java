package uk.ac.ebi.subs.api.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
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
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;

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
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.linksResponseField;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.paginationBlock;
import static uk.ac.ebi.subs.api.utils.ValidationSchemaHelper.generateMockChecklists;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@Category(DocumentationProducer.class)
@WithMockUser(username = "checklist_docs_usi_user", roles = {Helpers.TEAM_NAME, Helpers.ADMIN_TEAM_NAME})
public class ValidationSchemaDocumentation {

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

    @Autowired
    private ChecklistRepository checklistRepository;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        clearDatabases();
        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);

        checklistRepository.save(generateMockChecklists());
    }

    private void clearDatabases() {
        this.checklistRepository.deleteAll();
        this.dataTypeRepository.deleteAll();
    }

    @After
    public void tearDown() {
        clearDatabases();
    }

    @Test
    public void validationSchemaList() throws Exception {
        this.mockMvc.perform(
                get("/api/validationSchemas?page=1&size=10")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("validationSchemas-list",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(halLinks(),
                                        linkWithRel("self").description("This resource list"),
                                        linkWithRel("first").description("The first page in the resource list"),
                                        linkWithRel("next").description("The next page in the resource list"),
                                        linkWithRel("prev").description("The previous page in the resource list"),
                                        linkWithRel("last").description("The last page in the resource list")
                                ),
                                responseFields(
                                        fieldWithPath("_embedded.validationSchemas").description("Available validation schemas"),
                                        linksResponseField(),
                                        paginationBlock()
                                )


                        )
                );
    }

    @Test
    public void getSpecificValidationSchemaById() throws Exception {
        this.mockMvc.perform(
                get("/api/validationSchemas/{id}", "schema_for_dataTypeId_1")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("get-specific-validation-schema",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("id").ignored(),
                                        fieldWithPath("version").description("Represents the version number of this validation schema."),
                                        fieldWithPath("$schema").description("Version of the JSON schema standard used by this schema."),
                                        fieldWithPath("author").description("The author/owner of this schema."),
                                        fieldWithPath("required").description("The list of first level required attributes for this schema."),
                                        fieldWithPath("type").ignored(),
                                        fieldWithPath("title").description("Title of this validation schema."),
                                        fieldWithPath("description").description("Description of this validation schema."),
                                        fieldWithPath("definitions").ignored(),
                                        fieldWithPath("$async").ignored(),
                                        fieldWithPath("properties").ignored()
                                )
                        )
                );
    }
}
