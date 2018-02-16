package uk.ac.ebi.subs.api.services;

import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.templates.Capture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class SheetService {

    public void preProcessSheet(Sheet sheet){
        if (sheet.getTemplate() ==null) return;
        if (sheet.getRows() == null) return;


        sheet.removeEmptyRows();
        sheet.removeColumnsPastLastNonEmpty();
        this.ignoreCommentLines(sheet);
        this.guessHeader(sheet);
        this.mapHeadings(sheet);
    }

    public void ignoreCommentLines(Sheet sheet) {
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
    public void guessHeader(Sheet sheet) {
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
    public void mapHeadings(Sheet sheet) {
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
