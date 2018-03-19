package uk.ac.ebi.subs.api.sheetloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.hateoas.RelProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.services.ChainedValidationService;
import uk.ac.ebi.subs.api.services.SubmittableValidationDispatcher;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.model.templates.Capture;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.SheetRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

    private ObjectMapper objectMapper;
    private SheetRepository sheetRepository;
    private SubmittableValidationDispatcher submittableValidationDispatcher;

    private SheetBulkOps sheetBulkOps;

    public SheetLoaderService(
            Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap,
            SheetRepository sheetRepository,
            RelProvider relProvider,
            ObjectMapper objectMapper,
            SubmittableValidationDispatcher submittableValidationDispatcher,
            SheetBulkOps sheetBulkOps) {
        initSubmittableMaps(submittableRepositoryMap, relProvider);
        this.sheetRepository = sheetRepository;
        this.objectMapper = objectMapper;
        this.submittableValidationDispatcher = submittableValidationDispatcher;
        this.sheetBulkOps = sheetBulkOps;
    }

    public void loadSheet(Sheet sheet) {
        Assert.notNull(sheet);
        Assert.notNull(sheet.getSubmission());
        Assert.notNull(sheet.getRows());
        Assert.notNull(sheet.getTemplate());

        Template template = sheet.getTemplate();
        String targetType = template.getTargetType().toLowerCase();
        Class<? extends StoredSubmittable> targetTypeClass = this.submittableClassMap.get(targetType);
        SubmittableRepository repository = this.submittableRepositoryMap.get(targetTypeClass);
        String submissionId = sheet.getSubmission().getId();

        logger.debug("mapping {} for submission {} from sheet {}", targetType, submissionId, sheet.getId());

        Collection<Pair<Row, ? extends StoredSubmittable>> submittablesWithRows = convertToSubmittables(sheet, targetTypeClass);

        submittablesWithRows = sheetBulkOps.lookupExistingEntries(sheet.getSubmission(), submittablesWithRows, repository);

        List<Pair<Row, ? extends StoredSubmittable>> freshSubmittables = submittablesWithRows.stream().filter(p -> p.getSecond().getId() == null).collect(Collectors.toList());
        List<Pair<Row, ? extends StoredSubmittable>> existingSubmittables = submittablesWithRows.stream().filter(p -> p.getSecond().getId() != null).collect(Collectors.toList());

        sheetBulkOps.updateExistingSubmittables(existingSubmittables, repository);
        sheetBulkOps.insertNewSubmittables(freshSubmittables, repository);

        Optional<? extends StoredSubmittable> o = submittablesWithRows.stream()
                .filter(p -> p.getFirst().hasErrors() == false )
                .map(p -> p.getSecond())
                .findAny();

        if (o.isPresent()){
            //this will trigger validation of everything in the submission
            submittableValidationDispatcher.validateUpdate(o.get());
        }

        sheet.setStatus(SheetStatusEnum.Completed);
        sheet.setLastModifiedDate(new Date());
        sheetRepository.save(sheet);
    }

    protected List<Pair<Row, ? extends StoredSubmittable>> convertToSubmittables(Sheet sheet, Class<? extends StoredSubmittable> targetTypeClass) {
        List<Capture> columnMappings = mapColumns(
                sheet.getHeaderRow(),
                sheet.getTemplate().getColumnCaptures(),
                Optional.of(sheet.getTemplate().getDefaultCapture())
        );

        List<Pair<Row, ? extends StoredSubmittable>> submittables = sheet.getRows().stream()
                .map(row -> Pair.of(
                        row,
                        rowToSubmittable(
                                columnMappings,
                                targetTypeClass,
                                sheet.getSubmission(),
                                row,
                                sheet.getHeaderRow().getCells()
                        ))
                )
                .collect(Collectors.toList());
        return submittables;
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


    private StoredSubmittable rowToSubmittable(List<Capture> columnMappings, Class<? extends StoredSubmittable> targetTypeClass, Submission submission, Row row, List<String> headerRow) {
        JSONObject json = rowToDocument(row, columnMappings, headerRow);

        logger.debug("mapping row to doc {} {}", row, json);

        return documentToSubmittable(targetTypeClass, submission, row, json);
    }

    protected StoredSubmittable documentToSubmittable(Class<? extends StoredSubmittable> targetTypeClass, Submission submission, Row row, JSONObject json) {
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
