package uk.ac.ebi.subs.api.validators;

import lombok.Data;
import lombok.NonNull;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.api.services.SheetService;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.repos.SheetRepository;

import java.util.List;
import java.util.Optional;

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
            validateUpdate(sheet, storedVersion, errors);
        }

        if (sheet.getSubmission() != null && !operationControlService.isUpdateable(sheet.getSubmission())) {
            SubsApiErrors.resource_locked.addError(errors, "submission");
        }

    }

    public void validateCreate(Sheet sheet, Errors errors) {

        if(sheet.getTemplate() == null){
            SubsApiErrors.missing_field.addError(errors,"template");
        }
        if(sheet.getSubmission() == null){
            SubsApiErrors.missing_field.addError(errors,"submission");
        }

        List<Row> firstRows = sheet.getFirstRows();
        boolean hasContent = (firstRows != null && !firstRows.isEmpty());

        if (!hasContent) {
            SubsApiErrors.missing_field.addError(errors, "rows");
        }

        if (errors.hasErrors()) {
            return;
        }

        if (!errors.hasErrors()) {
            Optional<JSONObject> optionalJsonWithoutAlias = sheetService.parse(sheet)
                    .filter(json -> !hasStringAlias(json))
                    .findAny();

            if (optionalJsonWithoutAlias.isPresent()) {
                SubsApiErrors.invalid.addError(errors, "rows");
            }
        }

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

        if (String.class.isAssignableFrom(alias.getClass())) {
            return true;
        }

        return false;
    }

    public void validateUpdate(Sheet sheet, Sheet storedVersion, Errors errors) {

        if (!SheetStatusEnum.Draft.equals(storedVersion.getStatus())) {
            SubsApiErrors.resource_locked.addError(errors);
            return;
        }

        //these things should not ever change
        ValidationHelper.thingCannotChange(sheet.getTemplate(), storedVersion.getTemplate(), "template", errors);
        ValidationHelper.thingCannotChange(sheet.getSubmission(), storedVersion.getSubmission(), "submission", errors);
        ValidationHelper.thingCannotChange(sheet.getTeam(), storedVersion.getTeam(), "team", errors);

        //these should not change until we work on column re-mapping support
        ValidationHelper.thingCannotChange(sheet.getHeaderRowIndex(), storedVersion.getHeaderRowIndex(), "headerRowIndex", errors);
        ValidationHelper.thingCannotChange(sheet.getMappings(), storedVersion.getMappings(), "mappings", errors);
        ValidationHelper.thingCannotChange(sheet.getRows(), storedVersion.getRows(), "rows", errors);

        //this leaves the status as the only thing that can change


    }
}
