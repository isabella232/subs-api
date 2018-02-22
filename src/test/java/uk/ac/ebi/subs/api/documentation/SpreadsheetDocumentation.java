package uk.ac.ebi.subs.api.documentation;

import org.junit.After;
import org.junit.Assert;
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
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.model.templates.AttributeCapture;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.JsonFieldType;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.SheetRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.TemplateRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;

import java.util.List;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.addAuthTokenHeader;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.linksResponseField;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.selfRelLink;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@Category(DocumentationProducer.class)
@WithMockUser(username = "usi_admin_user", roles = {Helpers.TEAM_NAME, Helpers.ADMIN_TEAM_NAME})
public class SpreadsheetDocumentation {

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
    private TemplateRepository templateRepository;

    @Autowired
    private SheetRepository sheetRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;

    private MockMvc mockMvc;
    private Template template;
    private Submission submission;

    @Before
    public void setUp() {
        clearDatabases();
        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);
        this.submission = storeSubmission();

        template = Template.builder().name("default-sample-template").targetType("samples").build();
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

        templateRepository.insert(template);
    }


    private void clearDatabases() {
        this.sheetRepository.deleteAll();
        this.templateRepository.deleteAll();
        this.submissionRepository.deleteAll();
        this.submissionStatusRepository.deleteAll();
    }

    private Submission storeSubmission() {
        Submission sub = Helpers.generateTestSubmission();

        this.submissionStatusRepository.insert(sub.getSubmissionStatus());
        this.submissionRepository.save(sub);
        return sub;
    }

    @After
    public void tearDown() {
        clearDatabases();
    }

    @Test
    public void uploadSheet() throws Exception {
        Sheet sheet = uploadCsvAsSheet("sheet-csv-upload");
        Assert.assertEquals(SheetStatusEnum.Submitted, sheet.getStatus());
    }

    @Test
    public void uploadSheetTwice() throws Exception {
        uploadCsvAsSheet("sheet-csv-upload-rep-1");
        uploadCsvAsSheet("sheet-csv-upload-rep-2");
    }

    private Sheet uploadCsvAsSheet(String snippetName) throws Exception {
        final String comma = ",";

        String csv = String.join("\n",
                String.join(comma, "alias", "taxon id", "taxon", "height", "units"), //header
                String.join(comma, "s1", "9606", "Homo sapiens", "1.7", "meters"),
                String.join(comma, "s2", "9606", "Homo sapiens", "1.7", "meters")
        );


        this.mockMvc.perform(
                post("/api/submissions/{submissionId}/spreadsheet?templateName={templateName}",
                        submission.getId(),
                        template.getName())
                        .contentType("text/csv")
                        .accept(RestMediaTypes.HAL_JSON)
                        .content(csv)
        ).andExpect(status().isCreated())
                .andDo(
                        document(snippetName,
                                preprocessRequest(prettyPrint(), addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        linkWithRel("sheet").description("Link to the uploaded spreadsheet"),
                                        linkWithRel("submission").description("Link to the submission this upload is associated with"),
                                        linkWithRel("template").description("Link to the template used to process this data")
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("status").description("Current status of the batch of documents"),
                                        fieldWithPath("team").description("The team that owns this upload"),
                                        fieldWithPath("headerRow").description("The header row of this spreadsheet"),
                                        fieldWithPath("rows").description("The content of the spreadsheet"),
                                        fieldWithPath("totalRowCount").description("Number of documents in this batch"),
                                        fieldWithPath("processedRowCount").description("Number of documetns in this batch that have been loaded"),
                                        fieldWithPath("createdDate").ignored(),
                                        fieldWithPath("lastModifiedDate").ignored(),
                                        fieldWithPath("createdBy").ignored(),
                                        fieldWithPath("lastModifiedBy").ignored()
                                )
                        )
                );

        List<Sheet> batches = sheetRepository.findAll();
        return batches.get(0);
    }

}
