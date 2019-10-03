package uk.ac.ebi.subs.api.documentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mashape.unirest.http.HttpResponse;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.params.HttpParams;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.hypermedia.LinkDescriptor;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.api.services.SubmissionEventService;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;
import uk.ac.ebi.subs.repository.model.Submission;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

public class DocumentationHelper {

    public final static String AUTHORIZATION_HEADER_NAME = "Authorization";
    public final static String AUTHORIZATION_HEADER_VALUE = "Bearer '\"$TOKEN\"'";

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
            public void submissionSubmitted(SubmissionEnvelope submissionEnvelope
            ) {

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
        return DocumentationHelper.addHeader(AUTHORIZATION_HEADER_NAME,AUTHORIZATION_HEADER_VALUE);
    }

    protected static FieldDescriptor linksResponseField() {
        return fieldWithPath("_links").description("Links to other resources");
    }

    protected static LinkDescriptor selfRelLink() {
        return linkWithRel("self").description("Canonical link for this resource");
    }

    protected static FieldDescriptor paginationBlock(){
        return fieldWithPath("page").description("Pagination information including current page number, size of page, total elements and total pages availablee");
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

    protected static HttpResponse<String> convert(MvcResult mvcResult){
        return new HttpResponse<>(new MvcResultToHttpResponseAdapter(mvcResult),String.class);
    }

    private static class MvcResultToHttpResponseAdapter implements org.apache.http.HttpResponse {
        private MvcResult mvcResult;


        public MvcResultToHttpResponseAdapter(MvcResult mvcResult) {
            this.mvcResult = mvcResult;
        }

        @Override
        public StatusLine getStatusLine() {
            MvcResultToHttpResponseAdapter self = this;

            return new StatusLine(){
                @Override
                public ProtocolVersion getProtocolVersion() {
                    return self.getProtocolVersion();
                }

                @Override
                public int getStatusCode() {
                    return mvcResult.getResponse().getStatus();
                }

                @Override
                public String getReasonPhrase() {
                    return HttpStatus.valueOf(this.getStatusCode()).getReasonPhrase();
                }
            };
        }


        @Override
        public HttpEntity getEntity() {
            return new HttpEntity() {
                @Override
                public boolean isRepeatable() {
                    return false;
                }

                @Override
                public boolean isChunked() {
                    return false;
                }

                @Override
                public long getContentLength() {
                    return mvcResult.getResponse().getContentLengthLong();
                }

                @Override
                public Header getContentType() {
                    return null;
                }

                @Override
                public Header getContentEncoding() {
                    return null;
                }

                @Override
                public InputStream getContent() throws IOException, UnsupportedOperationException {
                    return new ByteArrayInputStream(mvcResult.getResponse().getContentAsByteArray());
                }

                @Override
                public void writeTo(OutputStream outstream) throws IOException {

                }

                @Override
                public boolean isStreaming() {
                    return false;
                }

                @Override
                public void consumeContent() throws IOException {

                }
            };
        }



        @Override
        public Locale getLocale() {
            return mvcResult.getResponse().getLocale();
        }



        @Override
        public ProtocolVersion getProtocolVersion() {
            return new ProtocolVersion("http",1,1);
        }

        @Override
        public boolean containsHeader(String name) {
            return false;
        }

        @Override
        public Header[] getHeaders(String name) {
            return new Header[0];
        }

        @Override
        public Header getFirstHeader(String name) {
            return null;
        }

        @Override
        public Header getLastHeader(String name) {
            return null;
        }

        @Override
        public void setLocale(Locale loc) {

        }

        @Override
        public Header[] getAllHeaders() {
            List<Header> headerList = new ArrayList<>();
            for (String headerName : mvcResult.getResponse().getHeaderNames()){
                headerList.add(new Header() {
                    @Override
                    public HeaderElement[] getElements() throws ParseException {
                        return new HeaderElement[0];
                    }

                    @Override
                    public String getName() {
                        return headerName;
                    }

                    @Override
                    public String getValue() {
                        return mvcResult.getResponse().getHeader(headerName);
                    }
                });
            }
            return headerList.toArray(new Header[0]);


        }

        @Override
        public void addHeader(Header header) {

        }

        @Override
        public void addHeader(String name, String value) {

        }

        @Override
        public void setHeader(Header header) {

        }

        @Override
        public void setHeader(String name, String value) {

        }

        @Override
        public void setHeaders(Header[] headers) {

        }

        @Override
        public void removeHeader(Header header) {

        }

        @Override
        public void removeHeaders(String name) {

        }

        @Override
        public HeaderIterator headerIterator() {
            return null;
        }

        @Override
        public HeaderIterator headerIterator(String name) {
            return null;
        }

        @Override
        public HttpParams getParams() {
            return null;
        }

        @Override
        public void setParams(HttpParams params) {

        }

        @Override
        public void setStatusLine(StatusLine statusline) {

        }

        @Override
        public void setStatusLine(ProtocolVersion ver, int code) {

        }

        @Override
        public void setStatusLine(ProtocolVersion ver, int code, String reason) {

        }

        @Override
        public void setStatusCode(int code) throws IllegalStateException {

        }

        @Override
        public void setReasonPhrase(String reason) throws IllegalStateException {

        }

        @Override
        public void setEntity(HttpEntity entity) {

        }

    }
}
