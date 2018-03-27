package uk.ac.ebi.subs.api.services;

import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;

import java.util.ListIterator;
import java.util.Optional;

@Component
public class SheetService {

    public void preProcessSheet(Sheet sheet) {
        if (sheet.getTemplate() == null) return;
        if (sheet.getRows() == null) return;


        sheet.removeEmptyRows();
        sheet.removeColumnsPastLastNonEmpty();
        this.dropCommentLines(sheet);
        this.guessHeader(sheet);
    }

    private void dropCommentLines(Sheet sheet) {
        ListIterator<Row> rowIterator = sheet.getRows().listIterator();

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            if (isfirstCharHash(row)) {
                rowIterator.remove();
            }
        }
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

        ListIterator<Row> rowIterator = sheet.getRows().listIterator();

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            Optional<Integer> r = row.columnIndexOflastNonEmptyCell();

            if (r.isPresent()) {
                sheet.setHeaderRow(row);
                rowIterator.remove();
                return;
            }
        }
    }


}
