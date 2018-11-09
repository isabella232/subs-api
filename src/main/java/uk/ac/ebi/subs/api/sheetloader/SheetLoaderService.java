package uk.ac.ebi.subs.api.sheetloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import uk.ac.ebi.subs.api.services.SubmittableValidationDispatcher;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.model.templates.Capture;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.SpreadsheetRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;
import uk.ac.ebi.subs.repository.services.SubmittableHelperService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This is a Spring @Service component for {@link Spreadsheet} entity.
 * It loads the {@link Spreadsheet} entity and saves it into the {@link SpreadsheetRepository}.
 */
@Service
@RequiredArgsConstructor
public class SheetLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(SheetLoaderService.class);

    @NonNull
    private Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap;

    @NonNull
    private ObjectMapper objectMapper;
    @NonNull
    private SpreadsheetRepository sheetRepository;
    @NonNull
    private SubmittableValidationDispatcher submittableValidationDispatcher;
    @NonNull
    private SheetBulkOps sheetBulkOps;
    @NonNull
    private DataTypeRepository dataTypeRepository;
    @NonNull
    private ChecklistRepository checklistRepository;
    @NonNull
    private SubmissionRepository submissionRepository;

    public void loadSheet(Spreadsheet sheet) {
        logger.info("processing sheet {}", sheet.getId());
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("init");

        Assert.notNull(sheet);
        Assert.notNull(sheet.getSubmissionId());
        Assert.notNull(sheet.getRows());
        Assert.notNull(sheet.getDataTypeId());
        Assert.notNull(sheet.getChecklistId());

        Checklist checklist = checklistRepository.findOne(sheet.getChecklistId());
        DataType dataType = dataTypeRepository.findOne(checklist.getDataTypeId());

        Template template = checklist.getSpreadsheetTemplate();

        String targetType = dataType.getSubmittableClassName();
        Class<? extends StoredSubmittable> targetTypeClass = this.submittableRepositoryMap.keySet()
                .stream()
                .filter(sc -> sc.getName().equals(targetType))
                .findAny()
                .get();

        SubmittableRepository repository = this.submittableRepositoryMap.get(targetTypeClass);
        String submissionId = sheet.getSubmissionId();
        Submission submission = submissionRepository.findOne(submissionId);

        logger.debug("mapping {} for submission {} from sheet {}", targetType, submissionId, sheet.getId());

        stopWatch.stop();
        stopWatch.start("convert");

        Collection<Pair<Row, ? extends StoredSubmittable>> submittablesWithRows = convertToSubmittables(
                sheet,
                targetTypeClass,
                template,
                submission,
                dataType
        );

        stopWatch.stop();
        stopWatch.start("lookup");

        submittablesWithRows = sheetBulkOps.lookupExistingEntries(submission, submittablesWithRows, repository);

        stopWatch.stop();
        stopWatch.start("organise");

        List<Pair<Row, ? extends StoredSubmittable>> freshSubmittables = submittablesWithRows.stream()
                .filter(p -> !p.getFirst().hasErrors())
                .filter(p -> p.getSecond().getId() == null)
                .collect(Collectors.toList());

        List<Pair<Row, ? extends StoredSubmittable>> existingSubmittables = submittablesWithRows.stream()
                .filter(p -> !p.getFirst().hasErrors())
                .filter(p -> p.getSecond().getId() != null)
                .collect(Collectors.toList());

        stopWatch.stop();
        stopWatch.start("update existing");

        sheetBulkOps.updateExistingSubmittables(existingSubmittables, repository);

        stopWatch.stop();
        stopWatch.start("progress update");

        sheet.setLastModifiedDate(new Date());
        sheetRepository.save(sheet);

        stopWatch.stop();
        stopWatch.start("insert new");

        sheetBulkOps.insertNewSubmittables(freshSubmittables, repository);

        stopWatch.stop();
        stopWatch.start("validation trigger");
        submittablesWithRows.stream()
                .filter(p -> p.getFirst().hasErrors() == false)
                .map(p -> p.getSecond())
                .forEach(submittable -> submittableValidationDispatcher.validateUpdate(submittable));

        stopWatch.stop();
        stopWatch.start("save progress");

        sheet.setStatus(SheetStatusEnum.Completed);
        sheet.setLastModifiedDate(new Date());
        sheetRepository.save(sheet);

        stopWatch.stop();
        logger.info(stopWatch.prettyPrint());

    }

    protected List<Pair<Row, ? extends StoredSubmittable>> convertToSubmittables(
            Spreadsheet sheet,
            Class<? extends StoredSubmittable> targetTypeClass,
            Template template,
            Submission submission,
            DataType dataType) {
        List<Capture> columnMappings = mapColumns(
                sheet.getHeaderRow(),
                template.getColumnCaptures(),
                Optional.of(template.getDefaultCapture())
        );

        List<Pair<Row, ? extends StoredSubmittable>> submittables = new LinkedList<>();

        for (Row row : sheet.getRows()) {
            StoredSubmittable storedSubmittable = rowToSubmittable(
                    columnMappings,
                    targetTypeClass,
                    submission,
                    row,
                    sheet.getHeaderRow().getCells(),
                    dataType
            );

            if (storedSubmittable != null) {
                Pair<Row, ? extends StoredSubmittable> pair = Pair.of(row, storedSubmittable);
                submittables.add(pair);
            }
        }

        return submittables;
    }

    protected List<Capture> mapColumns(Row headerRow, Map<String, Capture> columnCaptures, Optional<Capture> optionalDefaultCapture) {
        logger.debug("Mapping by headers {} to captures {}, with default {}", headerRow, columnCaptures, optionalDefaultCapture);
        Map<String, Capture> columnCapturesWithLowerCaseKeys = lowerCasedColumnCaptures(columnCaptures);

        columnCapturesWithLowerCaseKeys.entrySet().stream().forEach(entry ->
                entry.getValue().setDisplayName(entry.getKey())
        );

        List<Capture> capturePositions = new ArrayList<>(Collections.nCopies(headerRow.getCells().size(), null));

        List<String> headerRowCells = headerRow.getCells();
        int position = 0;

        while (position < headerRowCells.size()) {

            String currentHeader = headerRowCells.get(position);
            currentHeader = currentHeader.trim().toLowerCase();

            if (columnCapturesWithLowerCaseKeys.containsKey(currentHeader)) {
                Capture capture = columnCapturesWithLowerCaseKeys.get(currentHeader);

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

    private Map<String, Capture> lowerCasedColumnCaptures(Map<String, Capture> columnCaptures) {
        return columnCaptures.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue));
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

        if (hasAccession(jsonObject)) {
            row.getErrors().add("Please do no provide an accession");
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

    /**
     * submittables should not have an accession at this stage
     *
     * @param json
     * @return
     */
    private static boolean hasAccession(JSONObject json) {
        return json.has("accession");
    }


    private StoredSubmittable rowToSubmittable(List<Capture> columnMappings, Class<? extends StoredSubmittable> targetTypeClass, Submission submission, Row row, List<String> headerRow, DataType dataType) {
        JSONObject json = rowToDocument(row, columnMappings, headerRow);

        logger.debug("mapping row to doc {} {}", row, json);

        return documentToSubmittable(targetTypeClass, submission, row, json, dataType);
    }

    protected StoredSubmittable documentToSubmittable(Class<? extends StoredSubmittable> targetTypeClass, Submission submission, Row row, JSONObject json, DataType dataType) {
        StoredSubmittable submittable = null;

        if (row.getErrors().isEmpty()) {
            try {
                submittable = objectMapper.readValue(json.toString(), targetTypeClass);

                submittable.setSubmission(submission);
                submittable.setDataType(dataType);
                submittable.setTeam(submission.getTeam());

                // provide default team name for references
                String teamName = submission.getTeam().getName();

                submittable.refs()
                        .filter(ref -> ref.getTeam() == null)
                        .filter(ref -> ref.getAccession() == null)
                        .filter(ref -> ref.getAlias() != null)
                        .forEach(ref -> ref.setTeam(teamName));

                // create the denormalised references needed to chain validation
                SubmittableHelperService.fillInReferences(submittable);


            } catch (IOException e) {
                logger.error("IO exception while converting json to submittable class {}. JSON: {} ", targetTypeClass.getName(), json);
                row.getErrors().add("Unrecoverable error while converting row");
            }

            logger.debug("mapped doc to submittable {} {}", json, submittable);
        }
        return submittable;
    }
}
