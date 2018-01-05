package uk.ac.ebi.subs.api.validators;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.error.ApiError;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.model.templates.Capture;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.SheetRepository;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

@Component
@Data
public class SheetValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return Sheet.class.isAssignableFrom(clazz);
    }

    @NonNull
    private SheetRepository sheetRepository;

    @Override
    public void validate(Object target, Errors errors) {
        Sheet sheet = (Sheet)target;

        Sheet storedVersion = null;
        if (sheet.getId() != null){
            storedVersion = sheetRepository.findOne(sheet.getId());
        }

        if (storedVersion == null) {
            validateCreate(sheet,errors);
        }
        else {
            validateUpdate(sheet,storedVersion,errors);
        }

    }

    public void validateCreate(Sheet sheet, Errors errors){

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "template", "required", "template is required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "submission", "required", "submission is required");

        List<Row> firstRows = sheet.getFirstRows();
        boolean hasContent = ( firstRows != null && !firstRows.isEmpty());

        if (!hasContent){
            SubsApiErrors.missing_field.addError(errors,"rows");
        }

        //TODO cannot tell from template model if a column is required yet

    }
    public void validateUpdate(Sheet sheet, Sheet storedVersion, Errors errors){

        if (!SheetStatusEnum.Draft.equals(storedVersion.getStatus())){
            SubsApiErrors.resource_locked.addError(errors);
            return;
        }

        //these things should not ever change
        ValidationHelper.thingCannotChange(sheet.getTemplate(),storedVersion.getTemplate(),"template",errors);
        ValidationHelper.thingCannotChange(sheet.getSubmission(),storedVersion.getSubmission(),"submission",errors);
        ValidationHelper.thingCannotChange(sheet.getTeam(),storedVersion.getTeam(),"team",errors);

        //these should not change until we work on column re-mapping support
        ValidationHelper.thingCannotChange(sheet.getHeaderRowIndex(),storedVersion.getHeaderRowIndex(),"headerRowIndex",errors);
        ValidationHelper.thingCannotChange(sheet.getMappings(),storedVersion.getMappings(),"mappings",errors);
        ValidationHelper.thingCannotChange(sheet.getRows(),storedVersion.getRows(),"rows",errors);

        //this leaves the status as the only thing that can change




    }
}
