package uk.ac.ebi.subs.api;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.DocumentationProducer;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Use this class to create document snippets. Ascii docotor will weave them into html documents,
 * using the files in src/resources/docs/ascidocs
 *
 * @see <a href="https://github.com/EBISPOT/OLS/blob/master/ols-web/src/test/java/uk/ac/ebi/spot/ols/api/ApiDocumentation.java">OLS ApiDocumentation.java</a>
 * <p>
 * gives this
 * @see <a href="http://www.ebi.ac.uk/ols/docs/api">OLS API Docs<</a>
 * <p>
 * API documentation should learn from the excellent example at @see <a href="https://developer.github.com/v3/">GitHub</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@Category(DocumentationProducer.class)
public class ApiDocumentation {


    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");

    private RestDocumentationResultHandler document;

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    SubmissionStatusRepository submissionStatusRepository;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.document = document("{method-name}"
                ,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())
        );

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration(this.restDocumentation))
                .alwaysDo(this.document)
                .build();
    }

    @Test
    public void submissionsByDomain() throws Exception {
        this.submissionRepository.deleteAll();

        Submission sub = Helpers.generateTestSubmission();

        this.submissionStatusRepository.save(sub.getSubmissionStatus());
        this.submissionRepository.save(sub);


        this.mockMvc.perform(get("/api/domains/{domainName}/submissions", sub.getDomain().getName())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        document("submissions/by-domain",
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("Canonical link for this resource") //TODO
                                ),
                                responseFields(
                                        fieldWithPath("_links").description("Links to other resources"),
                                        fieldWithPath("_embedded.submissions").description("Submissions matching the domain name"),
                                        paginationPageSizeDescriptor(),
                                        paginationTotalElementsDescriptor(),
                                        paginationTotalPagesDescriptor(),
                                        paginationPageNumberDescriptor()
                                )
                        )
                );
    }

    private FieldDescriptor paginationPageNumberDescriptor() {
        return fieldWithPath("page.number").description("The page number");
    }

    private FieldDescriptor paginationTotalPagesDescriptor() {
        return fieldWithPath("page.totalPages").description("The total number of pages");
    }

    private FieldDescriptor paginationTotalElementsDescriptor() {
        return fieldWithPath("page.totalElements").description("The total number of resources");
    }

    private FieldDescriptor paginationPageSizeDescriptor() {
        return fieldWithPath("page.size").description("The number of resources in this page");
    }
/*
    @Test
    public void submissionById() throws Exception {
        this.submissionRepository.deleteAll();

        Submission sub = Helpers.generateTestFullSubmission();

        this.submissionRepository.save(sub);

        this.mockMvc.perform(get("/api/submissions/{id}",sub.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        document("submissions/by-id",
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("Canonical link for this resource"), //TODO
                                        linkWithRel("submission").description("Canonical link for this resource") //TODO
                                ),
                                responseFields(
                                        fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources"),
                                        fieldWithPath("submitter").description("User who created this submission"),
                                        fieldWithPath("domain").description("Domain this submission belongs to"),
                                        fieldWithPath("submissionDate").description("Date that this submission was submitted"),
                                        fieldWithPath("status").description("Submission status"),
                                        fieldWithPath("analyses").description("Analyses in this submission"),
                                        fieldWithPath("assays").description("Assays in this submission"),
                                        fieldWithPath("assayData").description("Assay data in this submission"),
                                        fieldWithPath("egaDacs").description("EGA DACs in this submission"),
                                        fieldWithPath("egaDacPolicies").description("EGA DAC Policies in this submission"),
                                        fieldWithPath("egaDatasets").description("EGA Datasets in this submission"),
                                        fieldWithPath("projects").description("Projects in this submission"),
                                        fieldWithPath("samples").description("Samples in this submission"),
                                        fieldWithPath("sampleGroups").description("Sample Groups in this submission"),
                                        fieldWithPath("studies").description("Studies in this submission"),
                                        fieldWithPath("protocols").description("Protocols in this submission")
                                )
                        )
                );
    }
*/

}
