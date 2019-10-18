package uk.ac.ebi.subs.api.error;

public class ChecklistNotFoundException extends RuntimeException {

    public ChecklistNotFoundException(String checklistId) {
        super(String.format("Checklist not exists with ID: %s", checklistId));
    }
}
