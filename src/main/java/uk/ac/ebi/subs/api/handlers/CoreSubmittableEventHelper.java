package uk.ac.ebi.subs.api.handlers;

import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.services.ChainedValidationService;
import uk.ac.ebi.subs.api.services.SubmittableValidationDispatcher;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.services.SubmittableHelperService;

@Component
@RepositoryEventHandler
public class CoreSubmittableEventHelper {

    private SubmittableHelperService submittableHelperService;
    private SubmittableValidationDispatcher submittableValidationDispatcher;
    private ChainedValidationService chainedValidationService;

    public CoreSubmittableEventHelper(SubmittableHelperService submittableHelperService, SubmittableValidationDispatcher submittableValidationDispatcher, ChainedValidationService chainedValidationService) {
        this.submittableHelperService = submittableHelperService;
        this.submittableValidationDispatcher = submittableValidationDispatcher;
        this.chainedValidationService = chainedValidationService;
    }

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
        /* Actions here should be also made in SheetLoader Service */
        submittableHelperService.processingStatusAndValidationResultSetUp(storedSubmittable);
        submittableValidationDispatcher.validateCreate(storedSubmittable);
        chainedValidationService.triggerChainedValidation(storedSubmittable);

    }

    @HandleBeforeSave
    public void beforeSave(StoredSubmittable storedSubmittable) {
        submittableHelperService.setTeamFromSubmission(storedSubmittable);
    }

    @HandleAfterSave
    public void validateOnSave(StoredSubmittable storedSubmittable) {
        /* Actions here should be also made in SheetLoader Service */
        submittableValidationDispatcher.validateUpdate(storedSubmittable);
        chainedValidationService.triggerChainedValidation(storedSubmittable);
    }

    @HandleAfterDelete
    public void validateOnDelete(StoredSubmittable storedSubmittable) {
        chainedValidationService.triggerChainedValidation(storedSubmittable);
    }
}
