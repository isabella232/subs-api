package uk.ac.ebi.subs.api.documentation;

import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.operation.OperationRequest;
import org.springframework.restdocs.operation.OperationRequestFactory;
import org.springframework.restdocs.operation.OperationResponse;
import org.springframework.restdocs.operation.OperationResponseFactory;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;

import java.util.LinkedHashMap;
import java.util.Map;

public class HeaderAddingPreprocessor implements OperationPreprocessor {

    private final OperationRequestFactory requestFactory = new OperationRequestFactory();

    private final OperationResponseFactory responseFactory = new OperationResponseFactory();

    private final Map<String, String> headersToAdd = new LinkedHashMap<>();


    @Override
    public OperationRequest preprocess(OperationRequest request) {
        return this.requestFactory.createFrom(request, addHeaders(request.getHeaders()));
    }

    @Override
    public OperationResponse preprocess(OperationResponse response) {
        return this.responseFactory.createFrom(response,
                addHeaders(response.getHeaders()));
    }

    private HttpHeaders addHeaders(HttpHeaders originalHeaders) {
        HttpHeaders processedHeaders = new HttpHeaders();
        processedHeaders.putAll(originalHeaders);

        for (Map.Entry<String, String> header : headersToAdd.entrySet()) {
            processedHeaders.add(header.getKey(), header.getValue());
        }

        return processedHeaders;
    }

    public void addHeader(String headerName, String headerValue){
        headersToAdd.put(headerName,headerValue);
    }
}
