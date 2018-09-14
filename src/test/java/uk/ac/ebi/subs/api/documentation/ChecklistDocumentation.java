package uk.ac.ebi.subs.api.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
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
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.templates.AttributeCapture;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.JsonFieldType;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;

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
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.selfRelLink;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@Category(DocumentationProducer.class)
@WithMockUser(username = "checklist_docs_usi_user", roles = {Helpers.TEAM_NAME, Helpers.ADMIN_TEAM_NAME})
public class ChecklistDocumentation {

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

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @Autowired
    private ChecklistRepository checklistRepository;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    private Template template;
    private Checklist checklist;

    @Before
    public void setUp() {
        clearDatabases();
        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);
        this.objectMapper = DocumentationHelper.mapper();

        this.checklist = new Checklist();
        this.checklist.setId("test-template");
        this.checklist.setDataTypeId("samples");

        template
                .add(
                        "alias",
                        FieldCapture.builder().fieldName("alias").build()
                )
                .add(
                        "taxon id",
                        FieldCapture.builder().fieldName("taxonId").fieldType(JsonFieldType.IntegerNumber).build()
                )
                .add(
                        "taxon",
                        FieldCapture.builder().fieldName("taxon").build()
                );

        template.setDefaultCapture(
                AttributeCapture.builder().build()
        );

        this.checklist.setSpreadsheetTemplate(template);

        checklistRepository.insert(checklist);
    }

    private void clearDatabases() {
        this.checklistRepository.deleteAll();
    }

    @After
    public void tearDown() {
        clearDatabases();
    }

    @Test
    public void templateList() throws Exception {
        this.mockMvc.perform(
                get("/api/templates")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("templates-list",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        linkWithRel("search").description("Search resource for UI Support items"),
                                        linkWithRel("profile").description("Profile")
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("_embedded.templates").description("Spreadsheet templates available"),
                                        paginationBlock()
                                )


                        )
                );
    }

    @Test
    public void templateSearchResources() throws Exception {
        this.mockMvc.perform(
                get("/api/templates/search")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("templates-search",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        linkWithRel("by-target-type").description("Search for templates with a specific target type"),
                                        linkWithRel("by-name").description("Find a template by its name"),
                                        linkWithRel("by-target-type-and-tags").description("Search for templates matching a target type and a set of tags")
                                ),
                                responseFields(
                                        linksResponseField()
                                )


                        )
                );
    }

    @Test
    public void templateFindByName() throws Exception {
        this.mockMvc.perform(
                get("/api/templates/search/findOneByName?name=test-template")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("test-template-one",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        linkWithRel("template").description("Link to spreadsheet template"),
                                        linkWithRel("spreadsheet-csv-download").description("Link to download a spreadsheet template")
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("name").description("Unique name for this template"),
                                        fieldWithPath("targetType").description("The type of item to be created from spreadsheets based on this template"),
                                        fieldWithPath("columnCaptures").description("Column names for the spreadsheet, and the definition of how to map values to JSON documents"),
                                        fieldWithPath("defaultCapture").description("Handler for any columns in the spreadsheet that aren't expected based on the template"),
                                        fieldWithPath("tags").description("Text tags to help users search for templates"),
                                        fieldWithPath("createdDate").ignored(),
                                        fieldWithPath("lastModifiedDate").ignored(),
                                        fieldWithPath("createdBy").ignored(),
                                        fieldWithPath("lastModifiedBy").ignored()
                                )
                        )
                );

    }

    @Test
    public void templateFindByType() throws Exception {
        this.mockMvc.perform(
                get("/api/templates/search/findByTargetType?targetType=samples")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("test-template-by-type",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink()
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("_embedded.templates").description("Templates matching the query parameter"),
                                        paginationBlock()
                                )
                        )
                );
    }
}
