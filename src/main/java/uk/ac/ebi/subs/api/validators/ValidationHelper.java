package uk.ac.ebi.subs.api.validators;


import org.springframework.validation.Errors;
import uk.ac.ebi.subs.data.status.StatusDescription;

import java.util.List;
import java.util.Optional;

public class ValidationHelper {

    /**
     * Is a status change permitted?
     *
     *
     *
     * @param targetStatusName
     * @param currentStatusName
     * @param statuses
     * @param fieldName
     * @param errors
     */

    public static void validateStatusChange(
            String targetStatusName,
            String currentStatusName,
            List<StatusDescription> statuses,
            String fieldName,
            Errors errors) {

        if (targetStatusName == null || currentStatusName == null) {
            return; //TODO out of scope for this method
        }

        if (currentStatusName.equals(targetStatusName)){
            return; //no change, no need to validate
        }

        Optional<StatusDescription> optionalCurrentStatus = statuses
                .stream()
                .filter(s -> s.getStatusName().equals(currentStatusName))
                .findFirst();

        if (!optionalCurrentStatus.isPresent()) {
            throw new IllegalStateException(
                    "Cannot validate status transition, stored status " + currentStatusName
                            + "is not in the processing status list " + statuses);
        }

        StatusDescription currentStatus = optionalCurrentStatus.get();

        if (!currentStatus.isUserTransitionPermitted(targetStatusName)) {
            errors.rejectValue(fieldName,
                    "illegalStateTransition",
                    "This status change is not permitted");
        }
    }

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
            errors.rejectValue(fieldName, "changeNotPermitted" + fieldName, fieldName + " cannot be changed");
        }
    }


}
