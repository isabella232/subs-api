package uk.ac.ebi.subs.api.validators;


import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

/**
 * Class for validation errors from within the Subs API
 * The API validators should only add errors from this class
 * Convienience methods are included to add errors for a field or resource
 *
 * All error codes should be documented in the API docs
 */
public enum SubsApiErrors {

    missing_field("This required field has not been set"),
    invalid("The formatting of this field is invalid"),
    resource_locked("The resource cannot be changed"),
    already_exists("Another resource with the same value already exists"),
    already_exists_and_not_completed("Another resource with the same alias already exists within the team and is not 'Completed'.");

    private String description;

    public String description() {
        return description;
    }

    SubsApiErrors(String description){
        this.description = description;
    }

    public void addError( Errors errors){
        errors.reject(this.name(),this.name());
    }

    public void addError(Errors errors, String field){
        errors.rejectValue(field,this.name(),this.name());
    }

    public static void rejectIfEmptyOrWhitespace(Errors errors, String field){
        missing_field.doRejectIfEmptyOrWhitespace(errors, field);
    }

    private void doRejectIfEmptyOrWhitespace(Errors errors, String field){
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, field, this.name(), this.name());
    }


}
