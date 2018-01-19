package uk.ac.ebi.subs.api.documentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.hypermedia.LinkDescriptor;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.api.services.SubmissionEventService;
import uk.ac.ebi.subs.repository.model.Submission;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

public class DocumentationHelper {

    protected static JUnitRestDocumentation jUnitRestDocumentation(){
        return new JUnitRestDocumentation("build/generated-snippets");
    }

    protected static SubmissionEventService fakeSubmissionEventService() {
        return new SubmissionEventService() {
            @Override
            public void submissionCreated(Submission submission) {

            }

            @Override
            public void submissionUpdated(Submission submission) {

            }

            @Override
            public void submissionDeleted(Submission submission) {

            }

            @Override
            public void submissionSubmitted(Submission submission) {

            }
        };
    }

    protected static ObjectMapper mapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return objectMapper;

    }

    protected static MockMvc mockMvc(WebApplicationContext context, MockMvcRestDocumentationConfigurer docConfig) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(docConfig)
                .defaultRequest(get("/").contextPath("/api"))
                .build();
    }

    protected static MockMvcRestDocumentationConfigurer docConfig(JUnitRestDocumentation restDocumentation, String scheme, String host, int port) {
        MockMvcRestDocumentationConfigurer docConfig = documentationConfiguration(restDocumentation);

        docConfig.uris()
                .withScheme(scheme)
                .withHost(host)
                .withPort(port);

        return docConfig;
    }

    protected static HeaderAddingPreprocessor addHeader(String headerName, String headerValue){
        HeaderAddingPreprocessor preprocessor = new HeaderAddingPreprocessor();
        preprocessor.addHeader(headerName,headerValue);
        return preprocessor;
    }

    protected static HeaderAddingPreprocessor addAuthTokenHeader(){
        return DocumentationHelper.addHeader("Authorization","Bearer $TOKEN");
    }

    protected static FieldDescriptor linksResponseField() {
        return fieldWithPath("_links").description("Links to other resources");
    }

    protected static LinkDescriptor selfRelLink() {
        return linkWithRel("self").description("Canonical link for this resource");
    }

    protected static FieldDescriptor paginationPageNumberDescriptor() {
        return fieldWithPath("page.number").description("The page number");
    }

    protected static FieldDescriptor paginationTotalPagesDescriptor() {
        return fieldWithPath("page.totalPages").description("The total number of pages");
    }

    protected static FieldDescriptor paginationTotalElementsDescriptor() {
        return fieldWithPath("page.totalElements").description("The total number of resources");
    }

    protected static FieldDescriptor paginationPageSizeDescriptor() {
        return fieldWithPath("page.size").description("The number of resources in this page");
    }

    protected static LinkDescriptor nextRelLink() {
        return linkWithRel("next").description("Next page of this resource");
    }

    protected static LinkDescriptor lastRelLink() {
        return linkWithRel("last").description("Last page for this resource");
    }

    protected static LinkDescriptor firstRelLink() {
        return linkWithRel("first").description("First page for this resource");
    }

    protected static LinkDescriptor prevRelLink() {
        return linkWithRel("prev").description("Previous page for this resource");
    }
}
