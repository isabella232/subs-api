package uk.ac.ebi.subs.api.validators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

@Component
@Data
public class SheetValidator implements Validator {

    @NonNull
    private SheetRepository sheetRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return Sheet.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Sheet batch = (Sheet)target;

        if (batch.getId() == null || sheetRepository.findOne(batch.getId()) == null ){
            //create
            if (batch.getSubmission() == null){
                SubsApiErrors.missing_field.addError(errors,"submission");
            }
            if (batch.getTemplate() == null){
                SubsApiErrors.missing_field.addError(errors,"template");
            }
            if (batch.getRows() == null || batch.getRows().isEmpty()){
                SubsApiErrors.missing_field.addError(errors,"rows");
            }
            if (batch.getHeaderRow() == null || batch.getHeaderRow().getCells().isEmpty()){
                SubsApiErrors.missing_field.addError(errors,"headerRow");
            }
        }
        else {
            //update
            SubsApiErrors.resource_locked.addError(errors);
        }

    }



}
