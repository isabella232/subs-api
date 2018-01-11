package uk.ac.ebi.subs.api.validators;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;
import uk.ac.ebi.subs.repository.repos.SheetRepository;

@Component
@Data
public class SheetDeleteValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return Sheet.class.isAssignableFrom(clazz);
    }

    @NonNull
    private SheetRepository sheetRepository;


    @Override
    public void validate(Object target, Errors errors) {
        Sheet sheet = (Sheet)target;

        Sheet storedSheet = sheetRepository.findOne(sheet.getId());

        if (sheet == null) {
            throw new ResourceNotFoundException();
        }

        if (!storedSheet.getStatus().equals(SheetStatusEnum.Draft)){
            SubsApiErrors.resource_locked.addError(errors);
        }
    }
}
