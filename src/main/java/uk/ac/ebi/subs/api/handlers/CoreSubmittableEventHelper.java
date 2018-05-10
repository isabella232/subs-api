package uk.ac.ebi.subs.api.handlers;

import lombok.NonNull;
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
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

@Component
@RepositoryEventHandler
public class CoreSubmittableEventHelper {

    @NonNull
    private SubmittableHelperService submittableHelperService;
    @NonNull
    private SubmittableValidationDispatcher submittableValidationDispatcher;
    @NonNull
    private ValidationResultRepository validationResultRepository;

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
    public void handleAfterSubmittableDeletion(StoredSubmittable storedSubmittable) {
        deleteRelatedValidationResult(storedSubmittable);
    }

    private void fillInReferenceWithDefaultTeam(StoredSubmittable storedSubmittable){
        String teamName = storedSubmittable.getTeam().getName();

        storedSubmittable.refs()
                .filter(r -> r.getTeam() == null)
                .filter(r -> r.getAlias() != null)
                .filter(r -> r.getAccession() == null)
                .forEach(r -> r.setTeam(teamName));

    }

    private void deleteRelatedValidationResult(StoredSubmittable storedSubmittable) {
        ValidationResult validationResult = storedSubmittable.getValidationResult();

        validationResultRepository.delete(validationResult);
    }
}
