package uk.ac.ebi.subs.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import uk.ac.ebi.subs.api.validators.SubsApiErrors;
import uk.ac.ebi.subs.repository.model.SubmittablesBatch;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.templates.Capture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class SheetService {

    public SubmittablesBatch batchFromSheet(Sheet sheet){
        preProcessSheet(sheet);
        SubmittablesBatch batch = new SubmittablesBatch();
        batch.setName(sheet.getSheetName());
        batch.setStatus("Submitted");
        batch.setTeam(sheet.getTeam());
        batch.setSubmission(sheet.getSubmission());

        List<SubmittablesBatch.Document> documents = parseRowsToDocuments(sheet);
        batch.setDocuments(documents);

        return batch;
    }


    private SubmittablesBatch.Document rowToDocument(int rowIndex, Row row, List<Capture> mappings, List<String> headers) {
        JSONObject jsonObject = new JSONObject();
        SubmittablesBatch.Document document = new SubmittablesBatch.Document();

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
                    document.addError(errorMessage);
                }
            }

        }

        if (!hasStringAlias(jsonObject)) {
            document.addError("Please provide an alias");
        }

        if (!document.getErrors().isEmpty()){
            document.setProcessed(true);
        }

        document.setDocument( stringToJsonNode(jsonObject.toString()) );

        return document;
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

    public static JsonNode stringToJsonNode(String jsonContent){
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = null;
        try {
            actualObj = mapper.readTree(jsonContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return actualObj;
    }

    private List<SubmittablesBatch.Document> parseRowsToDocuments(Sheet sheet) {

        List<String> headers = sheet.getRows().get(sheet.getHeaderRowIndex()).getCells();
        List<Capture> mappings = sheet.getMappings();
        List<Row> rows = sheet.getRows();

        int firstRowToRead = sheet.getHeaderRowIndex() + 1;
        int rowListSize = rows.size();

        List<SubmittablesBatch.Document> documents = new LinkedList<>();

        for (int i = firstRowToRead; i < rowListSize; i++) {
            Row row = rows.get(i);

            if (row.isIgnored()) continue;

            SubmittablesBatch.Document doc = rowToDocument(i, row, mappings, headers);
            documents.add(doc);
        }

        return documents;
    }

    private void preProcessSheet(Sheet sheet){
        if (sheet.getTemplate() ==null) return;
        if (sheet.getRows() == null) return;


        sheet.removeEmptyRows();
        sheet.removeColumnsPastLastNonEmpty();
        this.ignoreCommentLines(sheet);
        this.guessHeader(sheet);
        this.mapHeadings(sheet);
    }

    private void ignoreCommentLines(Sheet sheet) {
        sheet.getRows().stream().filter(row -> isfirstCharHash(row)).forEach(row -> row.setIgnored(true));
    }

    private boolean isfirstCharHash(Row row) {
        if (row == null) {
            return false;
        }

        if (row.getCells() == null) {
            return false;
        }

        if (row.getCells().size() < 1) {
            return false;
        }

        String cellValue = row.getCells().get(0);
        if (cellValue.startsWith("#")) {
            return true;
        }
        return false;
    }


    /**
     * Picks a header row in the sheet
     *
     * @param sheet
     */
    private void guessHeader(Sheet sheet) {
        sheet.setHeaderRowIndex(null);

        ListIterator<Row> rowIterator = sheet.getRows().listIterator();

        while (rowIterator.hasNext()) {
            int rowIndex = rowIterator.nextIndex();
            Row row = rowIterator.next();

            Optional<Integer> r = row.columnIndexOflastNonEmptyCell();

            if (r.isPresent()) {
                sheet.setHeaderRowIndex(rowIndex);
                return;
            }
        }
    }

    /**
     * Must have data and a valid row picked
     *
     * @param sheet
     */
    private void mapHeadings(Sheet sheet) {
        Map<String, Capture> columnCaptures = sheet.getTemplate().getColumnCaptures();
        columnCaptures.entrySet().stream().forEach(entry ->
                entry.getValue().setDisplayName(entry.getKey())
        );

        Optional<Capture> defaultCapture = Optional.of(sheet.getTemplate().getDefaultCapture());

        if (sheet.getHeaderRowIndex() == null) return;

        List<String> headerRow = sheet.getRows().get(sheet.getHeaderRowIndex()).getCells();

        Capture[] emptyCaptures = new Capture[headerRow.size()];

        List<Capture> capturePositions = new ArrayList<>(Arrays.asList(emptyCaptures));


        int position = 0;

        while (position < headerRow.size()) {

            String currentHeader = headerRow.get(position);
            currentHeader = currentHeader.trim().toLowerCase();


            if (columnCaptures.containsKey(currentHeader)) {
                Capture capture = columnCaptures.get(currentHeader);

                position = capture.map(position, capturePositions, headerRow);
            } else if (defaultCapture.isPresent()) {
                Capture clonedCapture = defaultCapture.get().copy();
                clonedCapture.setDisplayName(currentHeader);

                position = clonedCapture.map(position, capturePositions, headerRow);
            } else {
                position++;
            }

        }

        sheet.setMappings(capturePositions);
    }


}
