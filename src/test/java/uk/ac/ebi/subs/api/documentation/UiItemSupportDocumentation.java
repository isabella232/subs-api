package uk.ac.ebi.subs.api.documentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.DocumentationProducer;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.repository.model.UiSupportItem;
import uk.ac.ebi.subs.repository.repos.UiSupportItemRepository;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.linksResponseField;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.paginationBlock;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.selfRelLink;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@Category(DocumentationProducer.class)
@WithMockUser(username = "ui_support_item_usi_user", roles = {Helpers.TEAM_NAME, Helpers.ADMIN_TEAM_NAME})
public class UiItemSupportDocumentation {

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
    private UiSupportItemRepository uiSupportItemRepository;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        clearDatabases();
        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);
        this.objectMapper = DocumentationHelper.mapper();
    }

    private void clearDatabases() {
        this.uiSupportItemRepository.deleteAll();
    }

    @After
    public void tearDown() {
        clearDatabases();
    }

    @Test
    public void uiSupportItem() throws Exception {
        UiSupportItem uiSupportItem = new UiSupportItem();
        uiSupportItem.setName("sample-label-text");
        uiSupportItem.setValue(new TextNode("This is an example value"));

        uiSupportItemRepository.insert(uiSupportItem);

        this.mockMvc.perform(
                get("/api/uiSupportItems")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("uiSupportItems-list",
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
                                        subsectionWithPath("_embedded.uiSupportItems").description("Support data available"),
                                        paginationBlock()
                                )


                        )
                );


        this.mockMvc.perform(
                get("/api/uiSupportItems/search/findOneByName?name=sample-label-text")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("uiSupportItems-one",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        linkWithRel("uiSupportItem").description("Link to UI support item")
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("name").description("Unique name for this support item"),
                                        fieldWithPath("value").description("JSON value/document, used to support the UI"),
                                        fieldWithPath("createdDate").ignored(),
                                        fieldWithPath("lastModifiedDate").ignored(),
                                        fieldWithPath("createdBy").ignored(),
                                        fieldWithPath("lastModifiedBy").ignored()
                                )


                        )
                );


    }
}
