package uk.ac.ebi.subs.api.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
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
import java.util.regex.Pattern;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.replacePattern;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
        Sheet sheet = uploadCsvAsSheet();
        Assert.assertEquals(SheetStatusEnum.Draft, sheet.getStatus());
    }

    private Sheet uploadCsvAsSheet() throws Exception {
        final String comma = ",";

        String csv = String.join("\n",
                String.join(comma, "alias", "taxon id", "taxon", "height", "units"), //header
                String.join(comma, "s1", "9606", "Homo sapiens", "1.7", "meters"),
                String.join(comma, "s2", "9606", "Homo sapiens", "1.7", "meters")
        );


        this.mockMvc.perform(
                post("/api/submissions/{submissionId}/contents/samples/sheets?templateName={templateName}",
                        submission.getId(),
                        template.getName())
                        .contentType("text/csv")
                        .accept(RestMediaTypes.HAL_JSON)
                        .content(csv)
        ).andExpect(status().isCreated())
                .andDo(
                        document("sheet-csv-upload",
                                preprocessRequest(
                                        prettyPrint()                                ),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        linkWithRel("sheet").description("Link to this uploaded spreadsheet"),
                                        linkWithRel("submission").description("Link to the submission this upload is associated with")
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("headerRowIndex").description("Index of the row thought to contain the column headers"),
                                        fieldWithPath("status").description("Current status of the sheet"),
                                        fieldWithPath("template").description("The spreadsheet template this upload is based on"),
                                        fieldWithPath("team").description("The team that owns this upload"),
                                        fieldWithPath("rows").description("The spreadsheet content"),
                                        fieldWithPath("mappings").description("The column mappings determined for this spreadsheet"),
                                        fieldWithPath("firstRowsLimit").description("The number of rows to display when summarising this content"),
                                        fieldWithPath("_embedded.submission").description("Submission this spreadsheet was uploaded to"),
                                        fieldWithPath("createdDate").ignored(),
                                        fieldWithPath("lastModifiedDate").ignored(),
                                        fieldWithPath("createdBy").ignored(),
                                        fieldWithPath("lastModifiedBy").ignored()
                                )
                        )
                );

        List<Sheet> sheets = sheetRepository.findAll();
        return sheets.get(0);
    }

    @Test
    public void patchSheetStatus() throws Exception {
        Sheet sheet = uploadCsvAsSheet();

        this.mockMvc.perform(
                patch("/api/sheets/{sheetId}", sheet.getId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)
                        .content("{\"status\": \"Submitted\"}")
        ).andExpect(status().isOk())
                .andDo(
                        document("sheet-patch-status",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        linkWithRel("sheet").description("Link to this uploaded spreadsheet"),
                                        linkWithRel("submission").description("Link to the submission this upload is associated with")
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("headerRowIndex").description("Index of the row thought to contain the column headers"),
                                        fieldWithPath("status").description("Current status of the sheet"),
                                        fieldWithPath("template").description("The spreadsheet template this upload is based on"),
                                        fieldWithPath("team").description("The team that owns this upload"),
                                        fieldWithPath("rows").description("The spreadsheet content"),
                                        fieldWithPath("mappings").description("The column mappings determined for this spreadsheet"),
                                        fieldWithPath("firstRowsLimit").description("The number of rows to display when summarising this content"),
                                        fieldWithPath("_embedded.submission").description("Submission this spreadsheet was uploaded to"),
                                        fieldWithPath("createdDate").ignored(),
                                        fieldWithPath("lastModifiedDate").ignored(),
                                        fieldWithPath("createdBy").ignored(),
                                        fieldWithPath("lastModifiedBy").ignored()
                                )
                        )
                );
    }

    @Test
    public void patchSheetContents() throws Exception {
        Sheet sheet = uploadCsvAsSheet();

        this.mockMvc.perform(
                patch("/api/sheets/{sheetId}", sheet.getId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)
                        .content("{\"headerRowIndex\": 1}")
        ).andExpect(status().isBadRequest())
                .andDo(
                        document("sheet-patch-content",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint())
                        )
                );
    }

    @Test
    public void deleteSheet() throws Exception {
        Sheet sheet = uploadCsvAsSheet();

        this.mockMvc.perform(
                delete("/api/sheets/{sheetId}", sheet.getId())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isNoContent())
                .andDo(
                        document("sheet-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint())
                        )
                );
    }
}
