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
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.model.templates.AttributeCapture;
import uk.ac.ebi.subs.repository.model.templates.FieldCapture;
import uk.ac.ebi.subs.repository.model.templates.JsonFieldType;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.SpreadsheetRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;

import java.util.List;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.addAuthTokenHeader;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.linksResponseField;
import static uk.ac.ebi.subs.api.documentation.DocumentationHelper.selfRelLink;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    private ChecklistRepository checklistRepository;

    @Autowired
    private SpreadsheetRepository spreadsheetRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;

    private MockMvc mockMvc;
    private Template template;
    private Submission submission;
    private Checklist checklist;

    @Before
    public void setUp() {
        clearDatabases();
        MockMvcRestDocumentationConfigurer docConfig = DocumentationHelper.docConfig(restDocumentation, scheme, host, port);
        this.mockMvc = DocumentationHelper.mockMvc(this.context, docConfig);
        this.submission = storeSubmission();

        template = new Template();

        checklist = new Checklist();
        checklist.setId("simple-sample-template");
        checklist.setSpreadsheetTemplate(template);
        checklist.setDataTypeId("samples");

        //.builder().name("default-sample-template").targetType("samples").build();
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

        checklistRepository.insert(checklist);
    }


    private void clearDatabases() {
        this.spreadsheetRepository.deleteAll();
        this.checklistRepository.deleteAll();
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
        Spreadsheet sheet = uploadCsvAsSheet("sheet-csv-upload");
        Assert.assertEquals(SheetStatusEnum.Submitted, sheet.getStatus());
    }

    @Test
    public void uploadSheetTwice() throws Exception {
        uploadCsvAsSheet("sheet-csv-upload-rep-1");
        uploadCsvAsSheet("sheet-csv-upload-rep-2");
    }

    @Test
    public void fetchSheet() throws Exception {
        Spreadsheet sheet = new Spreadsheet();
        sheet.setStatus(SheetStatusEnum.Submitted);
        sheet.setChecklistId(checklist.getId());
        sheet.setDataTypeId(checklist.getDataTypeId());
        sheet.setSubmissionId(submission.getId());
        sheet.setTeam(submission.getTeam());
        sheet.setHeaderRow(new Row(headerCells));
        sheet.addRow(row1Cells);
        sheet.addRow(row2Cells);

        spreadsheetRepository.insert(sheet);

        this.mockMvc.perform(
                get("/api/spreadsheets/{id}",
                        sheet.getId())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("fetch-sheet",
                                preprocessRequest(prettyPrint(), addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        linkWithRel("spreadsheet").description("Link to the uploaded spreadsheet"),
                                        linkWithRel("submission").description("Link to the submission this upload is associated with"),
                                        linkWithRel("checklist").description("Link to the checklist used to process this data"),
                                        linkWithRel("dataType").description("Link to the data type definition for this data")
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("status").description("Current status of the batch of documents"),
                                        subsectionWithPath("team").description("The team that owns this upload"),
                                        subsectionWithPath("headerRow").description("The header row of this spreadsheet"),
                                        subsectionWithPath("rows").description("The content of the spreadsheet"),
                                        fieldWithPath("totalRowCount").description("Number of documents in this batch"),
                                        fieldWithPath("processedRowCount").description("Number of documetns in this batch that have been loaded"),
                                        fieldWithPath("createdDate").ignored(),
                                        fieldWithPath("lastModifiedDate").ignored(),
                                        fieldWithPath("createdBy").ignored(),
                                        fieldWithPath("lastModifiedBy").ignored()
                                )
                        )
                );


    }

    @Test
    public void uploadEmptyCsvExpectValidationError() throws Exception {
        this.mockMvc.perform(
                post("/api/submissions/{submissionId}/spreadsheet?checklistId={checklistId}",
                        submission.getId(),
                        checklist.getId())
                        .contentType("text/csv")
                        .accept(RestMediaTypes.HAL_JSON)
                        .content("")
        ).andExpect(status().isBadRequest())
                .andDo(
                        document("sheet-upload-error",
                                preprocessRequest(prettyPrint(), addAuthTokenHeader()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks()
                                ),
                                responseFields(
                                        fieldWithPath("type").ignored(),
                                        fieldWithPath("title").ignored(),
                                        fieldWithPath("status").ignored(),
                                        fieldWithPath("instance").ignored(),
                                        fieldWithPath("errors").ignored()
                                )
                        )
                );
    }

    private final String[] headerCells = new String[]{"alias", "taxon id", "taxon", "height", "units"};
    private final String[] row1Cells = new String[]{"s1", "9606", "Homo sapiens", "1.7", "meters"};
    private final String[] row2Cells = new String[]{"s2", "9606", "Homo sapiens", "1.7", "meters"};

    private Spreadsheet uploadCsvAsSheet(String snippetName) throws Exception {
        final String comma = ",";

        String csv = String.join("\n",
                String.join(comma, headerCells), //header
                String.join(comma, row1Cells),
                String.join(comma, row2Cells)
        );


        this.mockMvc.perform(
                post("/api/submissions/{submissionId}/spreadsheet?checklistId={checklistId}",
                        submission.getId(),
                        checklist.getId())
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
                                        linkWithRel("spreadsheet").description("Link to the uploaded spreadsheet"),
                                        linkWithRel("submission").description("Link to the submission this upload is associated with"),
                                        linkWithRel("checklist").description("Link to the checklist used to process this data"),
                                        linkWithRel("dataType").description("Link to the data type definition for this data")
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("status").description("Current status of the batch of documents"),
                                        subsectionWithPath("team").description("The team that owns this upload"),
                                        subsectionWithPath("headerRow").description("The header row of this spreadsheet"),
                                        subsectionWithPath("rows").description("The content of the spreadsheet"),
                                        fieldWithPath("totalRowCount").description("Number of documents in this batch"),
                                        fieldWithPath("processedRowCount").description("Number of documetns in this batch that have been loaded"),
                                        fieldWithPath("createdDate").ignored(),
                                        fieldWithPath("lastModifiedDate").ignored(),
                                        fieldWithPath("createdBy").ignored(),
                                        fieldWithPath("lastModifiedBy").ignored(),
                                        fieldWithPath("id").ignored(),
                                        fieldWithPath("version").ignored()
                                )
                        )
                );

        List<Spreadsheet> batches = spreadsheetRepository.findAll();
        return batches.get(0);
    }

}
