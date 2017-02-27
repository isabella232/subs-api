package uk.ac.ebi.subs.api.validators;


import org.springframework.validation.Errors;
import uk.ac.ebi.subs.data.status.StatusDescription;

import java.util.List;
import java.util.Optional;

public class ValidationHelper {


    public static void thingCannotChange(Object thing, Object storedThing, String fieldName, Errors errors) {
        boolean thingHasChanged = (
                thing != null
                        && storedThing != null
                        && !storedThing.equals(thing)
        );

        boolean thingHasBeenNulled = (
                thing == null
                        && storedThing != null
        );


        if (thingHasChanged || thingHasBeenNulled) {
            SubsApiErrors.resource_locked.addError(errors,fieldName);
        }

    }


}
