package uk.ac.ebi.subs.api.validators;

import lombok.Data;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.repos.SpreadsheetRepository;

@Component
@Data
public class SheetValidator implements Validator {

    @NonNull
    private SpreadsheetRepository sheetRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return Spreadsheet.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Spreadsheet batch = (Spreadsheet) target;

        if (batch.getId() == null || sheetRepository.findOne(batch.getId()) == null) {
            //create
            if (batch.getSubmissionId() == null) {
                SubsApiErrors.missing_field.addError(errors, "submissionId");
            }
            if (batch.getChecklistId() == null) {
                SubsApiErrors.missing_field.addError(errors, "checklistId");
            }
            if (batch.getRows() == null || batch.getRows().isEmpty()) {
                SubsApiErrors.missing_field.addError(errors, "rows");
            }
            if (batch.getHeaderRow() == null || batch.getHeaderRow().getCells().isEmpty()) {
                SubsApiErrors.missing_field.addError(errors, "headerRow");
            }
        } else {
            //update
            SubsApiErrors.resource_locked.addError(errors);
        }

    }


}
