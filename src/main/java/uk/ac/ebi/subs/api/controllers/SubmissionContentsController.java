package uk.ac.ebi.subs.api.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import uk.ac.ebi.subs.api.error.ChecklistNotFoundException;
import uk.ac.ebi.subs.api.processors.LinkHelper;
import uk.ac.ebi.subs.api.processors.StoredSubmittableAssembler;
import uk.ac.ebi.subs.api.processors.StoredSubmittableResourceProcessor;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.data.structures.SingleValidationResultStatus;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * This controller contains 3 endpoints related to submission elements.
 * It can retrieve all the given elements by the submission Id and data type Id.
 * It can create a new elements for the given submission by the given data type Id.
 * The summary endpoints can retrieve a summary for the errors/warnings by the given submission Id and data type Id.
 *
 */
@RestController
@RequiredArgsConstructor
public class SubmissionContentsController {

    @NonNull
    private DataTypeRepository dataTypeRepository;
    @NonNull
    private ChecklistRepository checklistRepository;
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

    /**
     * It can retrieve all the given elements by the submission Id and data type Id.
     * @param submissionId the Id of the submission
     * @param dataTypeId the Id of the data type
     * @param pageable the pagination information
     * @return all the given elements by the submission Id and data type Id.
     */
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

    /**
     * It can create a new elements for the given submission by the given data type Id.
     *
     * @param submissionId the Id of the submission
     * @param dataTypeId the Id of the data type
     * @param payload the object's data
     * @return a new elements for the given submission by the given data type Id.
     */
    @RequestMapping(value = "/submissions/{submissionId}/contents/{dataTypeId}", method = RequestMethod.POST)
    public ResponseEntity<Resource<StoredSubmittable>> createSubmissionContents(
            @PathVariable @P("submissionId") String submissionId,
            @PathVariable @P("dataTypeId") String dataTypeId,
            @RequestBody ObjectNode payload
    ) {
        DataType dataType = dataTypeRepository.findOne(dataTypeId);
        if (dataType == null) {
            throw new ResourceNotFoundException();
        }

        removeUnneededPayLoadElements(payload);

        Class<? extends StoredSubmittable> submittableClass = submittableClassForDataType(dataType);
        SubmittableRepository submittableRepository = submittableRepositoryMap.get(submittableClass);
        StoredSubmittable storedSubmittable = handleDataType(payload, dataType, submittableClass);

        handleChecklist(payload, storedSubmittable);
        handleSubmission(payload, submissionId, storedSubmittable);

        publisher.publishEvent(new BeforeCreateEvent(storedSubmittable));
        storedSubmittable = submittableRepository.insert(storedSubmittable);
        publisher.publishEvent(new AfterCreateEvent(storedSubmittable));

        Link selfLink = repositoryEntityLinks.linkToSingleResource(storedSubmittable);

        Resource<StoredSubmittable> resource = storedSubmittableAssembler.toResource(storedSubmittable);

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.LOCATION, selfLink.getHref());
        headers.add(HttpHeaders.ETAG, storedSubmittable.getVersion().toString());

        return new ResponseEntity<>(
                resource,
                headers,
                HttpStatus.CREATED
        );
    }

    private void removeUnneededPayLoadElements(@RequestBody ObjectNode payload) {
        payload.remove("submission");
        payload.remove("dataType");
    }

    private void handleSubmission(@RequestBody ObjectNode payload, String submissionId, StoredSubmittable item) {
        Submission submission = submissionRepository.findOne(submissionId);
        if (submission == null) {
            throw new ResourceNotFoundException();
        }

        item.setSubmission(submission);
    }

    private StoredSubmittable handleDataType(@RequestBody ObjectNode payload, DataType dataType,
                                             Class<? extends StoredSubmittable> submittableClass) {
        StoredSubmittable storedSubmittable;
        try {
            storedSubmittable = objectMapper.treeToValue(payload, submittableClass);
            storedSubmittable.setDataType(dataType);
        } catch (IOException e) {
            throw new RuntimeException(e); //refactor to validation error
        }

        return storedSubmittable;
    }

    private void handleChecklist(@RequestBody ObjectNode payload, StoredSubmittable storedSubmittable) {
        JsonNode checkListIdNode = payload.remove("checklistId");
        if (checkListIdNode == null) {
            return;
        }

        Checklist checklist = checklistRepository.findOne(checkListIdNode.asText());
        if (checklist == null) {
            throw new ChecklistNotFoundException(checkListIdNode.asText());
        }

        storedSubmittable.setChecklist(checklist);
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

    /**
     * The summary endpoints can retrieve a summary for the errors/warnings by the given submission Id and data type Id.
     * @param submissionId the Id of the submission
     * @param dataTypeId the Id of the data type
     * @return a summary for the errors/warnings by the given submission Id and data type Id.
     */
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