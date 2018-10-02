package uk.ac.ebi.subs.api.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.templates.AttributeCapture;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.JsonFieldType;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;

import java.io.IOException;

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

    @Autowired
    private DataTypeRepository dataTypeRepository;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    private Template template;
    private Checklist checklist;
    private DataType dataType;

    @Before
    public void setUp() throws IOException {
        clearDatabases();
        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);
        this.objectMapper = DocumentationHelper.mapper();

        this.dataType = new DataType();
        this.dataType.setId("samples");

        dataTypeRepository.insert(dataType);

        String jsonSchema = "{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\"\n}";


        this.checklist = new Checklist();
        this.checklist.setId("test-template");
        this.checklist.setDataTypeId(dataType.getId());
        this.checklist.setValidationSchema(objectMapper.readValue(jsonSchema,ObjectNode.class));
        this.checklist.setDisplayName("Simple samples");
        this.checklist.setDescription("Minimal set of requirements for a sample, including taxonomic information");


        this.template = new Template();
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
        this.dataTypeRepository.deleteAll();
    }

    @After
    public void tearDown() {
        clearDatabases();
    }

    @Test
    public void templateList() throws Exception {
        this.mockMvc.perform(
                get("/api/checklists")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("checklists-list",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        linkWithRel("search").description("Search resource for checklists items"),
                                        linkWithRel("profile").description("Profile")
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("_embedded.checklists").description("Checklists available"),
                                        paginationBlock()
                                )


                        )
                );
    }

    @Test
    public void templateSearchResources() throws Exception {
        this.mockMvc.perform(
                get("/api/checklists/search")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("checklists-search",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        linkWithRel("by-data-type-id").description("Search for templates with a specific target type")
                                ),
                                responseFields(
                                        linksResponseField()
                                )


                        )
                );
    }

    @Test
    public void get_one_checklist_by_id() throws Exception {
        this.mockMvc.perform(
                get("/api/checklists/{id}", checklist.getId())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("get-one-checklist",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        linkWithRel("spreadsheet-csv-download").description("Download the checklist as a spreadsheet"),
                                        linkWithRel("checklist").ignored()
                                ),
                                responseFields(
                                        fieldWithPath("id").ignored(),
                                        fieldWithPath("spreadsheetTemplate").description("description of spreadsheet columns and how to convert them to a JSON document"),
                                        fieldWithPath("dataTypeId").description("Data type that this checklist can be used with"),
                                        fieldWithPath("displayName").description("Human friendly name for the checklist"),
                                        fieldWithPath("description").description("Description of the checklist"),
                                        fieldWithPath("validationSchema").description("A JSON schema that will be applied to any documents using this checklist"),
                                        linksResponseField()
                                )


                        )
                );
    }


    @Test
    public void templateFindByType() throws Exception {
        this.mockMvc.perform(
                get("/api/checklists/search/by-data-type-id?dataTypeId=samples")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("checklists-by-dataType",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink()
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("_embedded.checklists").description("Checklists matching the query parameter"),
                                        paginationBlock()
                                )
                        )
                );
    }
}
