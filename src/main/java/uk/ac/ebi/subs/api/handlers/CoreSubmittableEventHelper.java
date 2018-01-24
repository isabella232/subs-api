package uk.ac.ebi.subs.api.handlers;

import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.services.SubmittableValidationDispatcher;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.services.SubmittableHelperService;

@Component
@RepositoryEventHandler
public class CoreSubmittableEventHelper {

    public CoreSubmittableEventHelper(SubmittableHelperService submittableHelperService, SubmittableValidationDispatcher submittableValidationDispatcher) {
        this.submittableHelperService = submittableHelperService;
        this.submittableValidationDispatcher = submittableValidationDispatcher;
    }

    private SubmittableHelperService submittableHelperService;
    private SubmittableValidationDispatcher submittableValidationDispatcher;

    /**
     * Give submittable an ID and set Team from submission.
     *
     * @param submittable
     */
    @HandleBeforeCreate
    public void addDependentObjectsToSubmittable(StoredSubmittable submittable) {
        submittableHelperService.uuidAndTeamFromSubmissionSetUp(submittable);
    }

    @HandleAfterCreate
    public void validateOnCreate(StoredSubmittable storedSubmittable) {
        submittableHelperService.processingStatusAndValidationResultSetUp(storedSubmittable);
        submittableValidationDispatcher.validateCreate(storedSubmittable);
    }

    @HandleBeforeSave
    public void beforeSave(StoredSubmittable storedSubmittable) {
        submittableHelperService.setTeamFromSubmission(storedSubmittable);
    }

    @HandleAfterSave
    public void validateOnSave(StoredSubmittable storedSubmittable) {
        submittableValidationDispatcher.validateUpdate(storedSubmittable);
    }
}
