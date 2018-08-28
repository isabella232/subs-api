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

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@Category(DocumentationProducer.class)
@WithMockUser(username = "usi_admin_user", roles = {Helpers.TEAM_NAME, Helpers.ADMIN_TEAM_NAME})
public class DataTypeDocumentation {

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

    @Before
    public void setUp() {
        clearDatabases();
        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);

        ApiIntegrationTestHelper.initialiseDataTypes(dataTypeRepository);
    }

    @After
    public void clearDatabases() {
        dataTypeRepository.deleteAll();
    }

    @Test
    public void list_data_types() throws Exception {

        this.mockMvc.perform(get("/api/dataTypes"))
                .andExpect(status().isOk())
                .andDo(document(
                        "list-data-types",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        links(
                                halLinks(),
                                linkWithRel("profile").ignored(),
                                linkWithRel("self").ignored()
                        ),
                        responseFields(
                                fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources"),
                                fieldWithPath("_embedded.dataTypes").description("page of data type descriptions"),
                                fieldWithPath("page").ignored()
                        )
                ));

    }

    @Test
    public void get_one_data_type() throws Exception {
        DataType dataType = dataTypeRepository.findAll().iterator().next();

        this.mockMvc.perform(get("/api/dataTypes/{id}", dataType.getId()))
                .andExpect(status().isOk())
                .andDo(document(
                        "get-data-type",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        links(
                                halLinks(),
                                linkWithRel("self").ignored(),
                                linkWithRel("dataType").ignored(),
                                linkWithRel("checklists").description("Link to checklists available for this data type")

                        ),
                        responseFields(
                                fieldWithPath("displayNameSingular").description("singular name for the data type"),
                                fieldWithPath("displayNamePlural").description("plural name for the data type"),
                                fieldWithPath("description").description("descritpion of the data type"),
                                fieldWithPath("archive").description("the archive in which this data will be stored"),
                                fieldWithPath("validationSchema").description("JSON schema used to validate this data typee"),
                                fieldWithPath("submittableClassName").description("Underlying representation for this data type"),
                                fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources")

                        )
                ));

    }
}


