package uk.ac.ebi.subs.api.validators;


import org.springframework.validation.Errors;

/**
 * Helper class to check if an object has changed or has been set to null.
 */
public class ValidationHelper {

    public static void thingCannotChange(Object thing, Object storedThing, String fieldName, Errors errors) {
        boolean thingHasChanged = (
                thing != null
                && storedThing != null
                && !storedThing.equals(thing)
        );

        boolean thingHasBeenNilled = (
                thing == null
                && storedThing != null
        );

        if (thingHasChanged || thingHasBeenNilled) {
            SubsApiErrors.resource_locked.addError(errors, fieldName);
        }
    }
}
