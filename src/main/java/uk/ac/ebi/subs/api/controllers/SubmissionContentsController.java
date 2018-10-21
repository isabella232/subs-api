package uk.ac.ebi.subs.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.processors.LinkHelper;
import uk.ac.ebi.subs.api.processors.StoredSubmittableAssembler;
import uk.ac.ebi.subs.api.processors.StoredSubmittableResourceProcessor;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.data.structures.SingleValidationResultStatus;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

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
    private ApplicationEventPublisher publisher;
    @NonNull
    private Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap;

    @NonNull
    private StoredSubmittableAssembler storedSubmittableAssembler;

    @NonNull
    private StoredSubmittableResourceProcessor<StoredSubmittable> storedSubmittableResourceProcessor;

    @NonNull
    private OperationControlService operationControlService;

    @NonNull
    private SubmissionRepository submissionRepository;

    @NonNull
    private ObjectMapper objectMapper;

    @NonNull
    private LinkHelper linkHelper;

    @NonNull
    private PagedResourcesAssembler pagedResourcesAssembler;


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/submissions/{submissionId}/contents/{dataTypeId}", method = RequestMethod.GET)
    public PagedResources<Resource<StoredSubmittable>> getSubmissionContentsForDataType(
            @PathVariable @P("submissionId") String submissionId,
            @PathVariable @P("dataTypeId") String dataTypeId,
            Pageable pageable) {

        Submission submission = submissionRepository.findOne(submissionId);
        DataType dataType = dataTypeRepository.findOne(dataTypeId);

        if (dataType == null || submission == null) {
            throw new ResourceNotFoundException();
        }

        Class submittableClass = submittableClassForDataType(dataType);
        SubmittableRepository repository = submittableRepositoryMap.get(submittableClass);

        Page<StoredSubmittable> page = repository.findBySubmissionIdAndDataTypeId(submissionId, dataTypeId, pageable);


        PagedResources pagedResources = pagedResourcesAssembler.toResource(
                page,
                storedSubmittableAssembler
        );


        addContentListPageLinks(pageable, submission, dataType, submittableClass, pagedResources);


        return pagedResources;
    }

    private void addContentListPageLinks(Pageable pageable, Submission submission, DataType dataType, Class submittableClass, PagedResources<Resource<StoredSubmittable>> pagedResources) {
        Map<String, String> params = new HashMap<>();
        params.put("submissionId", submission.getId());
        params.put("dataTypeId", dataType.getId());

        Link summaryLink = linkTo(methodOn(this.getClass()).summariseSubmissionDataTypeErrorStatus(submission.getId(), dataType.getId()))
                .withRel("validationSummaryCounts");

        Link checklistLink = repositoryEntityLinks
                .linkToSearchResource(Checklist.class, "by-data-type-id")
                .expand(params)
                .withRel("checklists");

        Link spreadsheetLink = repositoryEntityLinks
                .linkToSearchResource(Spreadsheet.class, "by-submission-and-data-type")
                .expand(params)
                .withRel("spreadsheets");

        Link dataTypeLink = repositoryEntityLinks.linkToSingleResource(dataType);

        Link withErrorsLink = repositoryEntityLinks.linkToSearchResource(submittableClass, "by-submission-and-data-type-with-errors")
                .expand(params)
                .withRel("documents-with-errors");

        Link withWarningsLink = repositoryEntityLinks.linkToSearchResource(submittableClass, "by-submission-and-data-type-with-warnings")
                .expand(params)
                .withRel("documents-with-warnings");

        pagedResources.add(
                checklistLink,
                spreadsheetLink,
                summaryLink,
                dataTypeLink,
                withErrorsLink,
                withWarningsLink
        );

        if (operationControlService.isUpdateable(submission)) {
            pagedResources.add(
                    linkHelper.submittableCreateLink(dataType, submission).withRel("create"),
                    linkHelper.spreadsheetUploadLink(submission)
            );
        }
    }


    @RequestMapping(value = "/submissions/{submissionId}/contents/{dataTypeId}", method = RequestMethod.POST)
    public ResponseEntity<Resource<StoredSubmittable>> createSubmissionContents(
            @PathVariable @P("submissionId") String submissionId,
            @PathVariable @P("dataTypeId") String dataTypeId,
            @RequestBody ObjectNode payload,
            HttpServletRequest originalRequest
    ) {

        //is it a real data type
        DataType dataType = dataTypeRepository.findOne(dataTypeId);

        if (dataType == null) {
            throw new ResourceNotFoundException();
        }

        Class<? extends StoredSubmittable> submittableClass = submittableClassForDataType(dataType);
        SubmittableRepository submittableRepository = submittableRepositoryMap.get(submittableClass);

        // is it a real submission
        Submission submission = submissionRepository.findOne(submissionId);

        if (submission == null) {
            throw new ResourceNotFoundException();
        }

        payload.remove("submission");
        payload.remove("dataType");

        //create the submittable
        StoredSubmittable item = null;
        try {
            item = objectMapper.treeToValue(payload, submittableClass);
        } catch (IOException e) {
            throw new RuntimeException(e); //refactor to validation error
        }

        item.setDataType(dataType);
        item.setSubmission(submission);


        publisher.publishEvent(new BeforeCreateEvent(item));
        item = submittableRepository.insert(item);
        publisher.publishEvent(new AfterCreateEvent(item));

        Link selfLink = repositoryEntityLinks.linkToSingleResource(item);

        Resource<StoredSubmittable> resource = storedSubmittableAssembler.toResource(item);

        resource = storedSubmittableResourceProcessor.process(resource);

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.LOCATION, selfLink.getHref());
        headers.add(HttpHeaders.ETAG, item.getVersion().toString());

        return new ResponseEntity<>(
                resource,
                headers,
                HttpStatus.CREATED
        );
    }

    private HttpHeaders responseHeaders(HttpResponse<String> response) {
        HttpHeaders responseHeaders = new HttpHeaders();

        Set<String> responseHeadersToKeep = new HashSet<>();
        responseHeadersToKeep.add(HttpHeaders.CACHE_CONTROL);
        responseHeadersToKeep.add(HttpHeaders.CONTENT_TYPE);
        responseHeadersToKeep.add(HttpHeaders.DATE);
        responseHeadersToKeep.add(HttpHeaders.EXPIRES);
        responseHeadersToKeep.add(HttpHeaders.PRAGMA);
        responseHeadersToKeep.add(HttpHeaders.LOCATION);
        responseHeadersToKeep.add(HttpHeaders.LINK);
        responseHeadersToKeep.add(HttpHeaders.LAST_MODIFIED);
        responseHeadersToKeep.add(HttpHeaders.ETAG);

        for (Map.Entry<String, List<String>> headerEntry : response.getHeaders().entrySet()) {
            if (responseHeadersToKeep.contains(headerEntry.getKey())) {
                responseHeaders.put(
                        headerEntry.getKey(),
                        headerEntry.getValue());
            }
        }
        return responseHeaders;
    }

    private Map<String, String> requestHeaders(HttpServletRequest originalRequest) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNamesEnum = originalRequest.getHeaderNames();

        Set<String> requestHeadersToSkip = new HashSet<>();
        requestHeadersToSkip.add(HttpHeaders.CONTENT_LENGTH.toLowerCase());


        while (headerNamesEnum.hasMoreElements()) {
            String headerName = headerNamesEnum.nextElement();

            if (!requestHeadersToSkip.contains(headerName.toLowerCase())) {
                String headerValue = originalRequest.getHeader(headerName);
                headers.put(headerName, headerValue);
            }
        }
        return headers;
    }

    private Class<? extends StoredSubmittable> submittableClassForDataType(DataType dataType) {
        Optional<Class<? extends StoredSubmittable>> submittableClass = submittableRepositoryMap.keySet().stream()
                .filter(clazz -> clazz.getName().equals(dataType.getSubmittableClassName()))
                .findAny();

        if (!submittableClass.isPresent()) {
            String message = String.format(
                    "Configuration error - data type %s specifies submittable class %s, but this is not available in the submittable class list: %s ",
                    dataType.getId(),
                    dataType.getSubmittableClassName(),
                    submittableRepositoryMap.keySet()
            );
            logger.error(message);
            throw new RuntimeException("A software configuration error prevents this request from succeeding");
        }

        return submittableClass.get();
    }

    @RequestMapping(value = "/submissions/{submissionId}/contents/{dataTypeId}/summary", method = RequestMethod.GET)
    public DataTypeSummary summariseSubmissionDataTypeErrorStatus(
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


    @Data
    public class DataTypeSummary {
        private long totalNumber;
        private long hasError;
        private long hasWarning;

        void increaseTotalNumberByOne() {
            this.totalNumber++;
        }

        void increaseErrorCountByOne() {
            this.hasError++;
        }

        void increaseWarningCountByOne() {
            this.hasWarning++;
        }
    }
}