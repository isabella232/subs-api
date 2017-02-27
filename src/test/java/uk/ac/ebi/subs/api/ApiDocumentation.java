package uk.ac.ebi.subs.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.hypermedia.LinkDescriptor;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.operation.preprocess.ContentModifier;
import org.springframework.restdocs.operation.preprocess.ContentModifyingOperationPreprocessor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.DocumentationProducer;
import uk.ac.ebi.subs.data.component.Domain;
import uk.ac.ebi.subs.data.component.Submitter;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
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

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    SubmissionStatusRepository submissionStatusRepository;

    @Autowired
    SampleRepository sampleRepository;

    @Autowired
    ProcessingStatusRepository processingStatusRepository;

    private ObjectMapper objectMapper;


    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;


    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        clearDatabases();

        MockMvcRestDocumentationConfigurer docConfig = documentationConfiguration(this.restDocumentation);

        docConfig.uris()
                .withScheme("http")
                .withHost("www.ebi.ac.uk/submissions")
                .withPort(80);

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(docConfig)
                .build();
    }

    private void clearDatabases() {
        this.submissionRepository.deleteAll();
        this.sampleRepository.deleteAll();
    }

    @After
    public void tearDown() {
        clearDatabases();
    }

    @Test
    public void invalidJson() throws Exception {


        this.mockMvc.perform(
                post("/api/submissions").content("Tyger Tyger, burning bright, In the forests of the night")
                        .contentType(RestMediaTypes.HAL_JSON)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isBadRequest())
                .andDo(
                        document("invalid-json",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(),
                                responseFields(
                                        fieldWithPath("cause").description("Cause of the error"),
                                        fieldWithPath("message").description("Error message")

                                )
                        )
                );

    }

    @Test
    public void jsonArrayInsteadOfObject() throws Exception {
        uk.ac.ebi.subs.data.Submission submission = goodClientSubmission();

        String jsonRepresentation = objectMapper.writeValueAsString(Arrays.asList(submission, submission));


        this.mockMvc.perform(
                post("/api/submissions").content(jsonRepresentation)
                        .contentType(RestMediaTypes.HAL_JSON)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isBadRequest())
                .andDo(
                        document("json-array-instead-of-object",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(),
                                responseFields(
                                        fieldWithPath("cause").description("Cause of the error"),
                                        fieldWithPath("message").description("Error message")

                                )
                        )
                );

    }

    private uk.ac.ebi.subs.data.Submission goodClientSubmission() {
        uk.ac.ebi.subs.data.Submission submission = new uk.ac.ebi.subs.data.Submission();
        submission.setDomain(new Domain());
        submission.getDomain().setName("my-team");
        submission.setSubmitter(new Submitter());
        submission.getSubmitter().setEmail("alice@test.org");
        return submission;
    }

    @Test
    public void invalidSubmission() throws Exception {
        uk.ac.ebi.subs.data.Submission submission = badClientSubmission();

        String jsonRepresentation = objectMapper.writeValueAsString(submission);


        this.mockMvc.perform(
                post("/api/submissions").content(jsonRepresentation)
                        .contentType(RestMediaTypes.HAL_JSON)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isBadRequest())
                .andDo(
                        document("invalid-submission",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("errors").description("List of errors"),
                                        fieldWithPath("errors[0].entity").description("Type of the entity with the error"),
                                        fieldWithPath("errors[0].property").description("Path of the field with the error"),
                                        fieldWithPath("errors[0].invalidValue").description("Value of the field that has caused the error"),
                                        fieldWithPath("errors[0].message").description("Message describing the error")

                                )
                        )
                );

    }

    private uk.ac.ebi.subs.data.Submission badClientSubmission() {
        return new uk.ac.ebi.subs.data.Submission();
    }

    private ContentModifyingOperationPreprocessor removeEmbedded() {
        return new ContentModifyingOperationPreprocessor(new RemoveEmbedded());
    }


    private class RemoveEmbedded implements ContentModifier {

        @Override
        public byte[] modifyContent(byte[] originalContent, MediaType contentType) {
            TypeReference<HashMap<String, Object>> typeRef
                    = new TypeReference<HashMap<String, Object>>() {
            };

            Map<String, Object> o = null;
            try {
                o = objectMapper.readValue(originalContent, typeRef);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            o.put("_embedded", "...");
            try {
                return objectMapper.writeValueAsBytes(o);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Test
    public void pageExample() throws Exception {

        String domainName = null;
        for (int i = 0; i < 50; i++) {
            Submission s = Helpers.generateTestSubmission();
            submissionStatusRepository.insert(s.getSubmissionStatus());
            submissionRepository.insert(s);
            domainName = s.getDomain().getName();
        }

        this.mockMvc.perform(get("/api/submissions/search/by-domain?domainName={domainName}&page=1&size=10", domainName))
                .andExpect(status().isOk())
                .andDo(document(
                        "page-example",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(removeEmbedded(), prettyPrint()),

                        links(halLinks(),
                                linkWithRel("self").description("This resource list"),
                                linkWithRel("first").description("The first page in the resource list"),
                                linkWithRel("next").description("The next page in the resource list"),
                                linkWithRel("prev").description("The previous page in the resource list"),
                                linkWithRel("last").description("The last page in the resource list")
                        ),
                        responseFields(
                                fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources"),
                                fieldWithPath("_embedded").description("The list of resources"),
                                fieldWithPath("page.size").description("The number of resources in this page"),
                                fieldWithPath("page.totalElements").description("The total number of resources"),
                                fieldWithPath("page.totalPages").description("The total number of pages"),
                                fieldWithPath("page.number").description("The page number")
                        )
                ));
    }

    @Test
    public void conditionalRequests() throws Exception {
        Submission sub = storeSubmission();
        List<Sample> samples = storeSamples(sub, 1);
        Sample s = samples.get(0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM YYYY H:m:s zzz");

        String etagValueString = String.format("ETag: \"%d\"",s.getVersion());
        String lastModifiedString = dateFormat.format(s.getLastModifiedDate());

        this.mockMvc.perform(
                get("/api/samples/{sampleId}", s.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-None-Match", etagValueString)
        ).andExpect(status().isNotModified())
                .andDo(
                        document("conditional-fetch-etag-get-if-none-match",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()
                                )
                        )
                );

        this.mockMvc.perform(
                delete("/api/samples/{sampleId}", s.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-Match", "ETag: \"10\"")
        ).andExpect(status().isPreconditionFailed())
                .andDo(
                        document("conditional-delete-if-etag-match",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()
                                )
                        )
                );

        this.mockMvc.perform(
                get("/api/samples/{sampleId}", s.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-Modified-Since", lastModifiedString)
        ).andExpect(status().isNotModified())
                .andDo(
                        document("conditional-fetch-if-modified-since",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()
                                )
                        )
                );


    }


    @Test
    public void sampleList() throws Exception {
        Submission sub = storeSubmission();
        List<Sample> samples = storeSamples(sub, 30);

        this.mockMvc.perform(
                get("/api/samples/search/by-submission?submissionId={submissionId}&size=2", sub.getId())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("samples/by-submission",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        nextRelLink(),
                                        firstRelLink(),
                                        lastRelLink()
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("_embedded.samples").description("Samples within the submission"),
                                        paginationPageSizeDescriptor(),
                                        paginationTotalElementsDescriptor(),
                                        paginationTotalPagesDescriptor(),
                                        paginationPageNumberDescriptor()
                                )
                        )
                );

        this.mockMvc.perform(
                get("/api/samples/{sample}", samples.get(0).getId())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("samples/fetch-one",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        processingStatusLink(),
                                        submissionLink(),
                                        linkWithRel("sample").description("Link to this sample")
                                ),
                                responseFields( //TODO fill out the descriptions
                                        linksResponseField(),
                                        fieldWithPath("alias").description(""),
                                        fieldWithPath("title").description(""),
                                        fieldWithPath("description").description(""),
                                        fieldWithPath("sampleRelationships").description(""),
                                        fieldWithPath("taxonId").description(""),
                                        fieldWithPath("taxon").description(""),
                                        fieldWithPath("attributes").description(""),
                                        fieldWithPath("createdDate").description(""),
                                        fieldWithPath("lastModifiedDate").description(""),
                                        fieldWithPath("createdBy").description(""),
                                        fieldWithPath("lastModifiedBy").description(""),
                                        fieldWithPath("_embedded.submission").description("")


                                )
                        ));
    }

    private List<Sample> storeSamples(Submission sub, int numberRequired) {
        List<Sample> samples = Helpers.generateTestSamples(numberRequired);

        for (Sample s : samples) {
            s.setCreatedDate(new Date());
            s.setSubmission(sub);
            processingStatusRepository.insert(s.getProcessingStatus());
            sampleRepository.insert(s);
        }
        return samples;
    }

    @Test
    public void rootEndpoint() throws Exception {

        this.mockMvc.perform(
                get("/api")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("root-endpoint",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        linkWithRel("processingStatuses").description(""),
                                        linkWithRel("projects").description(""),
                                        linkWithRel("egaDacs").description(""),
                                        linkWithRel("profile").description(""),
                                        linkWithRel("assays").description(""),
                                        linkWithRel("processingStatusDescriptions").description(""),
                                        linkWithRel("samples").description(""),
                                        linkWithRel("analyses").description(""),
                                        linkWithRel("assayData").description(""),
                                        linkWithRel("egaDacPolicies").description(""),
                                        linkWithRel("releaseStatusDescriptions").description(""),
                                        linkWithRel("submissions").description(""),
                                        linkWithRel("studies").description(""),
                                        linkWithRel("egaDatasets").description(""),
                                        linkWithRel("submissionStatuses").description(""),
                                        linkWithRel("protocols").description(""),
                                        linkWithRel("submissionStatusDescriptions").description(""),
                                        linkWithRel("sampleGroups").description("")

                                ),
                                responseFields(
                                        linksResponseField()
                                )
                        )
                );
    }

    @Test
    public void submissionsByDomain() throws Exception {

        Submission sub = storeSubmission();


        this.mockMvc.perform(
                get("/api/submissions/search/by-domain?domainName={domainName}", sub.getDomain().getName())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("submissions/by-domain",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink()
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("_embedded.submissions").description("Submissions matching the domain name"),
                                        paginationPageSizeDescriptor(),
                                        paginationTotalElementsDescriptor(),
                                        paginationTotalPagesDescriptor(),
                                        paginationPageNumberDescriptor()
                                )
                        )
                );
    }

    private FieldDescriptor linksResponseField() {
        return fieldWithPath("_links").description("Links to other resources");
    }

    private LinkDescriptor selfRelLink() {
        return linkWithRel("self").description("Canonical link for this resource");
    }

    private Submission storeSubmission() {
        Submission sub = Helpers.generateTestSubmission();

        this.submissionStatusRepository.save(sub.getSubmissionStatus());
        this.submissionRepository.save(sub);
        return sub;
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

    private LinkDescriptor nextRelLink() {
        return linkWithRel("next").description("Next page of this resource");
    }

    private LinkDescriptor lastRelLink() {
        return linkWithRel("last").description("Last page for this resource");
    }

    private LinkDescriptor firstRelLink() {
        return linkWithRel("first").description("First page for this resource");
    }

    private LinkDescriptor prevRelLink() {
        return linkWithRel("prev").description("Previous page for this resource");
    }

    private LinkDescriptor submissionLink() {
        return linkWithRel("submission").description("Submission in which this record was created");
    }

    private LinkDescriptor processingStatusLink() {
        return linkWithRel("processingStatus").description("Current status of this record");
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
