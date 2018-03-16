package uk.ac.ebi.subs.api.sheetloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.hateoas.RelProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.validation.ObjectError;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.model.templates.Capture;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.SheetRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;
import uk.ac.ebi.subs.repository.services.SubmittableHelperService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SheetLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(SheetLoaderService.class);

    private Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap;
    private Map<String, Class<? extends StoredSubmittable>> submittableClassMap;
    private ApplicationEventPublisher publisher;
    private ObjectMapper objectMapper;
    private SheetRepository sheetRepository;
    private SubmittableHelperService submittableHelperService;

    public SheetLoaderService(
            Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap,
            ApplicationEventPublisher publisher,
            SheetRepository sheetRepository,
            RelProvider relProvider,
            ObjectMapper objectMapper,
            SubmittableHelperService submittableHelperService) {
        initSubmittableMaps(submittableRepositoryMap, relProvider);
        this.publisher = publisher;
        this.sheetRepository = sheetRepository;
        this.objectMapper = objectMapper;
        this.submittableHelperService = submittableHelperService;
    }

    public void loadSheet(Sheet sheet) {
        Assert.notNull(sheet);
        Assert.notNull(sheet.getSubmission());
        Assert.notNull(sheet.getRows());
        Assert.notNull(sheet.getTemplate());

        Template template = sheet.getTemplate();
        String targetType = template.getTargetType().toLowerCase();
        String submissionId = sheet.getSubmission().getId();

        logger.debug("mapping {} for submission {} from sheet {}", targetType, submissionId, sheet.getId());

        List<Capture> columnMappings = mapColumns(
                sheet.getHeaderRow(),
                template.getColumnCaptures(),
                Optional.of(template.getDefaultCapture())
        );

        convertAndStore(sheet, columnMappings);


    }

    protected List<Capture> mapColumns(Row headerRow, Map<String, Capture> columnCaptures, Optional<Capture> optionalDefaultCapture) {
        logger.debug("Mapping by headers {} to captures {}, with default {}", headerRow, columnCaptures, optionalDefaultCapture);

        columnCaptures.entrySet().stream().forEach(entry ->
                entry.getValue().setDisplayName(entry.getKey())
        );

        List<Capture> capturePositions = new ArrayList<>(Collections.nCopies(headerRow.getCells().size(), null));

        List<String> headerRowCells = headerRow.getCells();
        int position = 0;

        while (position < headerRowCells.size()) {

            String currentHeader = headerRowCells.get(position);
            currentHeader = currentHeader.trim().toLowerCase();

            if (columnCaptures.containsKey(currentHeader)) {
                Capture capture = columnCaptures.get(currentHeader);

                position = capture.map(position, capturePositions, headerRowCells);
            } else if (optionalDefaultCapture.isPresent()) {
                Capture clonedCapture = optionalDefaultCapture.get().copy();
                clonedCapture.setDisplayName(currentHeader);
                position = clonedCapture.map(position, capturePositions, headerRowCells);
            } else {
                position++;
            }
        }

        capturePositions.forEach(capture ->
                capture.setDisplayName(null)
        );

        return capturePositions;
    }

    protected JSONObject rowToDocument(Row row, List<Capture> mappings, List<String> headers) {
        JSONObject jsonObject = new JSONObject();
        row.getErrors().clear();

        List<String> cells = row.getCells();
        ListIterator<Capture> captureIterator = mappings.listIterator();

        while (captureIterator.hasNext()) {
            int position = captureIterator.nextIndex();
            Capture capture = captureIterator.next();

            if (capture != null) {
                try {
                    capture.capture(position, headers, cells, jsonObject);
                } catch (NumberFormatException e) {
                    String errorMessage = capture.getDisplayName() + " must be a number";
                    row.getErrors().add(errorMessage);
                }
            }

        }

        if (!hasStringAlias(jsonObject)) {
            row.getErrors().add("Please provide an alias");
        }

        if (row.getErrors().isEmpty()) {
            row.setProcessed(true);
        }

        return jsonObject;
    }

    /**
     * all submittables must have an alias, which must be a non-null string
     *
     * @param json
     * @return
     */
    private static boolean hasStringAlias(JSONObject json) {
        if (!json.has("alias")) return false;

        Object alias = json.get("alias");

        if (alias == null) return false;

        if (String.class.isAssignableFrom(alias.getClass()) &&
                !alias.toString().trim().isEmpty()) {
            return true;
        }

        return false;
    }

    private void convertAndStore(Sheet sheet, List<Capture> columnMappings) {
        logger.debug("starting convert and store for sheet {}", sheet);
        String targetTypeName = sheet.getTemplate().getTargetType().toLowerCase();
        Class<? extends StoredSubmittable> targetTypeClass = this.submittableClassMap.get(targetTypeName);


        Assert.notNull(targetTypeClass);

        List<String> headerRow = sheet.getHeaderRow().getCells();
        Submission submission = sheet.getSubmission();

        List<Row> rowsToLoad = sheet.getRows().stream()
                .filter(row -> !row.isProcessed())
                .collect(Collectors.toList());

        int numberProcessed = 0;

        for (Row row : rowsToLoad) {

            convertAndStoreRow(columnMappings, targetTypeClass, submission, row, headerRow);

            row.setProcessed(true);
            numberProcessed++;
            if (numberProcessed % 100 == 0) {
                sheet.setLastModifiedDate(new Date());
                sheetRepository.save(sheet);
            }

        }
        logger.debug("triggering validation sheet {}", sheet);

        sheet.setStatus(SheetStatusEnum.Completed);
        sheet.setLastModifiedDate(new Date());
        sheetRepository.save(sheet);
        logger.debug("completed mapping of sheet {}", sheet);

    }

    protected void convertAndStoreRow(List<Capture> columnMappings, Class<? extends StoredSubmittable> targetTypeClass, Submission submission, Row row, List<String> headerRow) {

        StoredSubmittable submittable = rowToSubmittable(columnMappings, targetTypeClass, submission, row, headerRow);

        storeSubmittable(submission, row, submittable);
    }

    protected void storeSubmittable(Submission submission, Row row, StoredSubmittable submittable) {
        SubmittableRepository repository = this.submittableRepositoryMap.get(submittable.getClass());

        if (row.getErrors().isEmpty()) {
            StoredSubmittable storedVersion = repository.findOneBySubmissionIdAndAlias(submission.getId(), submittable.getAlias());

            logger.debug("alias: {} storedVersion: {}", submittable.getAlias(), storedVersion);

            try {
                if (storedVersion == null) {
                    createNewSubmittable(submittable, repository, row);
                } else {
                    updateExistingSubmittable(submittable, repository, row, storedVersion);
                }
            } catch (RepositoryConstraintViolationException exception) {
                for (ObjectError objectError : exception.getErrors().getAllErrors()) {
                    row.getErrors().add(objectError.getDefaultMessage());
                }
            }
        }
    }

    private StoredSubmittable rowToSubmittable(List<Capture> columnMappings, Class<? extends StoredSubmittable> targetTypeClass, Submission submission, Row row, List<String> headerRow) {
        JSONObject json = rowToDocument(row, columnMappings, headerRow);

        logger.debug("mapping row to doc {} {}", row, json);

        StoredSubmittable submittable = null;

        if (row.getErrors().isEmpty()) {
            try {
                submittable = objectMapper.readValue(json.toString(), targetTypeClass);
                submittable.setSubmission(submission);
            } catch (IOException e) {
                logger.error("IO exception while converting json to submittable class {}. JSON: {} ", targetTypeClass.getName(), json);
                row.getErrors().add("Unrecoverable error while converting row");
            }

            logger.debug("mapped doc to submittable {} {}", json, submittable);
        }
        return submittable;
    }

    private void updateExistingSubmittable(StoredSubmittable submittable, SubmittableRepository repository, Row row, StoredSubmittable storedVersionSubmittable) {
        submittable.setId(storedVersionSubmittable.getId());
        submittable.setVersion(storedVersionSubmittable.getVersion());
        submittable.setCreatedBy(storedVersionSubmittable.getCreatedBy());
        submittable.setCreatedDate(storedVersionSubmittable.getCreatedDate());

        logger.debug("Updating submittable {}", submittable);

        publisher.publishEvent(new BeforeSaveEvent(submittable));
        Object savedObject = repository.save(submittable);
        /* The obvious thing to do is publisher.publishEvent(new AfterSaveEvent(savedObject))
            , but this re-validates the whole submission at present, which gives unacceptable performance
            , so we run validation for the whole submission at the END of loading

         */

        /* Actions here should be also made to SheetLoader Service, but be careful about performance */
        logger.debug("Updating submittable {}", savedObject);


    }

    private void createNewSubmittable(StoredSubmittable submittable, SubmittableRepository repository, Row row) {
        logger.debug("Creating submittable {}", submittable);

        publisher.publishEvent(new BeforeCreateEvent(submittable));
        Object savedObject = repository.insert(submittable);

        /* The obvious thing to do is publisher.publishEvent(new AfterCreateEvent(savedObject))
            , but this re-validates the whole submission at present, which gives unacceptable performance
            , so we run validation for the whole submission at the END of loading

         */

        /* Actions here should be also made to SheetLoader Service, but be careful about performance */

        submittableHelperService.processingStatusAndValidationResultSetUp(submittable);

        logger.debug("Created submittable {}", savedObject);


    }

    private void initSubmittableMaps(Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap, RelProvider relProvider) {
        Map<String, Class<? extends StoredSubmittable>> classesByCollectionName = new HashMap<>();

        for (Map.Entry<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> entry : submittableRepositoryMap.entrySet()) {
            Class<? extends StoredSubmittable> submittableClass = entry.getKey();

            String collectionName = relProvider.getCollectionResourceRelFor(submittableClass);

            classesByCollectionName.put(collectionName, submittableClass);
        }

        this.submittableRepositoryMap = submittableRepositoryMap;
        this.submittableClassMap = classesByCollectionName;
    }
}
