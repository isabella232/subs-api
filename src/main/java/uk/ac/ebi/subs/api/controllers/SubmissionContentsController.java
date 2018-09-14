package uk.ac.ebi.subs.api.controllers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.services.Http;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.data.structures.SingleValidationResultStatus;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
public class SubmissionContentsController {

    @NonNull
    private DataTypeRepository dataTypeRepository;
    @NonNull
    private ValidationResultRepository validationResultRepository;
    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;
    @NonNull
    private List<Class<? extends StoredSubmittable>> submittableClassList;

    @NonNull
    private Http http;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/submissions/{submissionId}/contents/{dataTypeId}", method = RequestMethod.POST)
    public ResponseEntity createSubmissionContents(
            @PathVariable @P("submissionId") String submissionId,
            @PathVariable @P("dataTypeId") String dataTypeId,
            @RequestBody String payload,
            HttpServletRequest originalRequest
    ) {

        DataType dataType = dataTypeRepository.findOne(dataTypeId);

        if (dataType == null) {
            throw new ResourceNotFoundException();
        }

        Optional<Class<? extends StoredSubmittable>> submittableClass = submittableClassList.stream()
                .filter(clazz -> clazz.getName().equals(dataType.getSubmittableClassName()))
                .findAny();

        if (!submittableClass.isPresent()) {
            String message = String.format(
                    "Configuration error - data type %s specifies submittable class %s, but this is not available in the submittable class list: %s ",
                    dataType.getId(),
                    dataType.getSubmittableClassName(),
                    submittableClassList
            );
            logger.error(message);
            throw new RuntimeException("A software configuration error prevents this request from succeeding");
        }


        Link submittableCollectionLink = repositoryEntityLinks.linkToCollectionResource(submittableClass.get())
                .expand()
                .withSelfRel();


        try {
            JSONObject jsonPayload = new JSONObject(payload);
            Link submissionLink = repositoryEntityLinks.linkForSingleResource(Submission.class, submissionId).withSelfRel();
            Link dataTypeLink = repositoryEntityLinks.linkToSingleResource(DataType.class, dataTypeId).withSelfRel();
            jsonPayload.put("dataType", dataTypeLink.getHref());
            jsonPayload.put("submission", submissionLink.getHref());

            Map<String, String> headers = new LinkedHashMap<>();
            Enumeration<String> headerNamesEnum = originalRequest.getHeaderNames();

            Set<String> requestHeadersToSkip = new HashSet<>();
            requestHeadersToSkip.add(HttpHeaders.CONTENT_LENGTH.toLowerCase());

            Set<String> responseHeadersToSkip = new HashSet<>();
            responseHeadersToSkip.add(HttpHeaders.TRANSFER_ENCODING.toLowerCase());

            while (headerNamesEnum.hasMoreElements()) {
                String headerName = headerNamesEnum.nextElement();

                if (!requestHeadersToSkip.contains(headerName.toLowerCase())) {
                    String headerValue = originalRequest.getHeader(headerName);
                    headers.put(headerName, headerValue);
                }
            }

            HttpResponse<String> response = http.post(
                    submittableCollectionLink.getHref(),
                    headers,
                    jsonPayload.toString()
            );

            HttpHeaders responseHeaders = new HttpHeaders();

            for (Map.Entry<String, List<String>> headerEntry : response.getHeaders().entrySet()) {
                if (!responseHeadersToSkip.contains(headerEntry.getKey().toLowerCase())) {
                    responseHeaders.put(
                            headerEntry.getKey(),
                            headerEntry.getValue());
                }
            }

            return new ResponseEntity<>(
                    response.getBody(),
                    responseHeaders,
                    HttpStatus.valueOf(response.getStatus())
            );

        } catch (JSONException e) {
            throw new HttpMessageNotReadableException(
                    String.format("Could not read an object of type %s from the request!",
                            (submittableClass.isPresent()) ? submittableClass.get().getName() : "<unknown>"
                    )

            );
        } catch (UnirestException e) {
            logger.error("UniRestException when proxing request to spring data rest controller", e);
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(value = "/submissions/{submissionId}/contents/{dataTypeId}/summary", method = RequestMethod.GET)
    public DataTypeSummary createSubmissionContents(
            @PathVariable @P("submissionId") String submissionId,
            @PathVariable @P("dataTypeId") String dataTypeId
    ) {
        Stream<ValidationResult> streamOfValidationResults =
                validationResultRepository.findBySubmissionIdAndDataTypeId(submissionId, dataTypeId);
        DataTypeSummary dataTypeSummary = new DataTypeSummary();

        streamOfValidationResults.forEach(validationResult -> {
            dataTypeSummary.increaseTotalNumberByOne();
            if (validationResult.getOverallValidationOutcomeByAuthor()
                    .containsValue(SingleValidationResultStatus.Error.name())) {
                dataTypeSummary.increaseErrorCountByOne();
            }
            if (validationResult.getOverallValidationOutcomeByAuthor()
                    .containsValue(SingleValidationResultStatus.Warning.name())) {
                dataTypeSummary.increaseWarningCountByOne();
            }
        });

        return dataTypeSummary;
    }

    public class DataTypeSummary {
        private long totalNumber;
        private long hasError;
        private long hasWarning;

        public long getTotalNumber() {
            return this.totalNumber;
        }

        public void setTotalNumber(long totalNumber) {
            this.totalNumber = totalNumber;
        }

        void increaseTotalNumberByOne() {
            this.totalNumber++;
        }

        public long getHasError() {
            return hasError;
        }

        public void setHasError(long hasError) {
            this.hasError = hasError;
        }

        void increaseErrorCountByOne() {
            this.hasError++;
        }

        public long getHasWarning() {
            return hasWarning;
        }

        public void setHasWarning(long hasWarning) {
            this.hasWarning = hasWarning;
        }

        void increaseWarningCountByOne() {
            this.hasWarning++;
        }
    }
}