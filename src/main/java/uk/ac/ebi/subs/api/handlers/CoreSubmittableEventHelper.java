package uk.ac.ebi.subs.api.handlers;

import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.services.SubmittableValidationDispatcher;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.services.SubmittableHelperService;

@Component
@RepositoryEventHandler
public class CoreSubmittableEventHelper {

    private SubmittableHelperService submittableHelperService;
    private SubmittableValidationDispatcher submittableValidationDispatcher;

    public CoreSubmittableEventHelper(SubmittableHelperService submittableHelperService, SubmittableValidationDispatcher submittableValidationDispatcher) {
        this.submittableHelperService = submittableHelperService;
        this.submittableValidationDispatcher = submittableValidationDispatcher;
    }

    /**
     * Give submittable an ID and set Team from submission.
     *
     * @param submittable
     */
    @HandleBeforeCreate
    public void addDependentObjectsToSubmittable(StoredSubmittable submittable) {
        submittableHelperService.uuidAndTeamFromSubmissionSetUp(submittable);
        fillInReferenceWithDefaultTeam(submittable);
    }

    @HandleAfterCreate
    public void validateOnCreate(StoredSubmittable storedSubmittable) {
        /* Actions here should be also made in SheetLoader Service */
        submittableHelperService.processingStatusAndValidationResultSetUp(storedSubmittable);
        submittableValidationDispatcher.validateCreate(storedSubmittable);

    }

    @HandleBeforeSave
    public void beforeSave(StoredSubmittable storedSubmittable) {
        submittableHelperService.setTeamFromSubmission(storedSubmittable);
        fillInReferenceWithDefaultTeam(storedSubmittable);
    }

    @HandleAfterSave
    public void validateOnSave(StoredSubmittable storedSubmittable) {
        /* Actions here should be also made in SheetLoader Service */
        submittableValidationDispatcher.validateUpdate(storedSubmittable);
    }

    @HandleAfterDelete
    public void validateOnDelete(StoredSubmittable storedSubmittable) {
        // TODO - send validation event on deletion
    }

    private void fillInReferenceWithDefaultTeam(StoredSubmittable storedSubmittable){
        String teamName = storedSubmittable.getTeam().getName();

        storedSubmittable.refs()
                .filter(r -> r.getTeam() == null)
                .filter(r -> r.getAlias() != null)
                .filter(r -> r.getAccession() == null)
                .forEach(r -> r.setTeam(teamName));

    }
}
