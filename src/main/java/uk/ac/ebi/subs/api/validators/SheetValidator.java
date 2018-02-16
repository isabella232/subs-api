package uk.ac.ebi.subs.api.validators;

import lombok.Data;
import lombok.NonNull;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.api.services.SheetService;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.templates.Capture;
import uk.ac.ebi.subs.repository.repos.SheetRepository;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

@Component
@Data
public class SheetValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return Sheet.class.isAssignableFrom(clazz);
    }

    @NonNull
    private SheetRepository sheetRepository;

    @NonNull
    private SheetService sheetService;

    @NonNull
    private OperationControlService operationControlService;

    @Override
    public void validate(Object target, Errors errors) {
        Sheet sheet = (Sheet) target;

        Sheet storedVersion = null;
        if (sheet.getId() != null) {
            storedVersion = sheetRepository.findOne(sheet.getId());
        }

        if (storedVersion == null) {
            validateCreate(sheet, errors);
        } else {
            SubsApiErrors.resource_locked.addError(errors);
        }

        if (sheet.getSubmission() != null && !operationControlService.isUpdateable(sheet.getSubmission())) {
            SubsApiErrors.resource_locked.addError(errors, "submission");
        }

    }

    public void validateCreate(Sheet sheet, Errors errors) {

        if (sheet.getTemplate() == null) {
            SubsApiErrors.missing_field.addError(errors, "template");
        }
        if (sheet.getSubmission() == null) {
            SubsApiErrors.missing_field.addError(errors, "submission");
        }

        List<Row> firstRows = sheet.getFirstRows();
        boolean hasContent = (firstRows != null && !firstRows.isEmpty());

        if (!hasContent) {
            SubsApiErrors.missing_field.addError(errors, "rows");
        }

        if (errors.hasErrors()) {
            return;
        }

        parseRowsToDocuments(sheet, errors);

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

    public void parseRowsToDocuments(Sheet sheet, Errors errors) {

        List<String> headers = sheet.getRows().get(sheet.getHeaderRowIndex()).getCells();
        List<Capture> mappings = sheet.getMappings();
        List<Row> rows = sheet.getRows();

        int firstRowToRead = sheet.getHeaderRowIndex() + 1;
        int rowListSize = rows.size();

        for (int i = firstRowToRead; i < rowListSize; i++) {
            Row row = rows.get(i);

            if (row.isIgnored()) continue;

            JSONObject document = rowToDocument(i, row, mappings, headers, errors);
            row.setDocument(document.toString());
            row.setCells(Collections.emptyList());
        }
    }

    private JSONObject rowToDocument(int rowIndex, Row row, List<Capture> mappings, List<String> headers, Errors errors) {
        JSONObject document = new JSONObject();

        List<String> cells = row.getCells();
        ListIterator<Capture> captureIterator = mappings.listIterator();

        while (captureIterator.hasNext()) {
            int position = captureIterator.nextIndex();
            Capture capture = captureIterator.next();

            if (capture != null) {
                try {
                    capture.capture(position, headers, cells, document);
                } catch (NumberFormatException e) {
                    String fieldPath = "rows[" + rowIndex + "].cells[" + position + "]";
                    SubsApiErrors.invalid.addError(errors, fieldPath);
                }
            }

        }

        if (!hasStringAlias(document)) {
            String fieldPath = "rows[" + rowIndex + "]";
            SubsApiErrors.missing_alias.addError(errors, fieldPath);
        }

        return document;
    }
}
