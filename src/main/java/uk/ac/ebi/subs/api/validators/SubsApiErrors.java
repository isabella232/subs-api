package uk.ac.ebi.subs.api.validators;


import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

/**
 * Class for validation errors from within the Subs API.
 * The API validators should only add errors from this class.
 * Convenience methods are included to add errors for a field or resource
 *
 * All error codes should be documented in the API docs
 */
public enum SubsApiErrors {

    not_a_subs_team("Submissions must be owned by a 'subs.' team"),
    missing_alias("Each entry must have an alias"),
    missing_field("This required field has not been set"),
    unknown_accession("You have provided an accession, but we do not have a record of it."),
    inconsistent_with_previous_records("The value of this field conflicts with earlier submissions"),
    invalid("The formatting of this field is invalid"),
    resource_locked("The resource cannot be changed"),
    already_exists("Another resource with the same value already exists"),

    file_is_not_in_deletable_status("The file is currently not in a deletable status."),
    missing_profile_attribute("The team profile is missing a required attribute.");

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

    private void doRejectIfEmptyOrWhitespace(Errors errors, String field) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, field, this.name(), this.name());
    }
}
