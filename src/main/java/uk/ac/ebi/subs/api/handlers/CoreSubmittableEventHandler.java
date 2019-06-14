package uk.ac.ebi.subs.api.handlers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.model.StoredSubmittableDeleteMessage;
import uk.ac.ebi.subs.api.services.SubmittableValidationDispatcher;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;
import uk.ac.ebi.subs.repository.services.SubmittableHelperService;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import java.util.Map;

/**
 * This class responsible for handling Spring framework specific events
 * for {@link uk.ac.ebi.subs.data.submittable.Submittable} object(s) creation, save and deletion.
 */
@Component
@RepositoryEventHandler
@RequiredArgsConstructor
public class CoreSubmittableEventHandler {

    @NonNull
    private RabbitMessagingTemplate rabbitMessagingTemplate;
    @NonNull
    private SubmittableHelperService submittableHelperService;
    @NonNull
    private SubmittableValidationDispatcher submittableValidationDispatcher;
    @NonNull
    private ValidationResultRepository validationResultRepository;

    @NonNull
    private ProcessingStatusRepository processingStatusRepository;

    @NonNull
    private Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap;

    public final static String STORED_SUBMITTABLE_DELETION_ROUTING_KEY = "usi.submittable.deletion";

    /**
     * Give submittable an ID and set Team from submission and add all the referenced {@link uk.ac.ebi.subs.data.submittable.Submittable}
     * Add {@link uk.ac.ebi.subs.data.component.Team} for all referenced {@link StoredSubmittable} entity.
     * @param storedSubmittable
     */
    @HandleBeforeCreate
    public void addDependentObjectsToSubmittable(StoredSubmittable storedSubmittable) {
        submittableHelperService.uuidAndTeamFromSubmissionSetUp(storedSubmittable);
        submittableHelperService.fillInReferences(storedSubmittable);
        fillInReferenceWithDefaultTeam(storedSubmittable);
    }

    /**
     * Creates new {@link ProcessingStatus} and {@link ValidationResult} instances
     * and sets it on the {@link StoredSubmittable} entity.
     * Also sends an event that a {@link StoredSubmittable} entity has been created.
     * @param storedSubmittable the {@link StoredSubmittable} entity
     */
    @HandleAfterCreate
    public void validateOnCreate(StoredSubmittable storedSubmittable) {
        /* Actions here should be also made in SheetLoader Service */
        submittableHelperService.processingStatusAndValidationResultSetUp(storedSubmittable);

        SubmittableRepository repository = submittableRepositoryMap.get(storedSubmittable.getClass());
        repository.save(storedSubmittable);

        submittableValidationDispatcher.validateCreate(storedSubmittable);
    }

    /**
     * Sets {@link uk.ac.ebi.subs.data.component.Team} for the {@link StoredSubmittable} entity.
     * Add {@link uk.ac.ebi.subs.data.component.Team} for all referenced {@link StoredSubmittable} entity.
     * Add all the referenced {@link uk.ac.ebi.subs.data.submittable.Submittable}
     * @param storedSubmittable the {@link StoredSubmittable} entity
     */
    @HandleBeforeSave
    public void beforeSave(StoredSubmittable storedSubmittable) {
        submittableHelperService.setTeamFromSubmission(storedSubmittable);
        fillInReferenceWithDefaultTeam(storedSubmittable);
        submittableHelperService.fillInReferences(storedSubmittable);
    }

    /**
     * Sends an event that a {@link StoredSubmittable} entity has been updated.
     * @param storedSubmittable the {@link StoredSubmittable} entity
     */
    @HandleAfterSave
    public void validateOnSave(StoredSubmittable storedSubmittable) {
        /* Actions here should be also made in SheetLoader Service */
        submittableValidationDispatcher.validateUpdate(storedSubmittable);
    }

    /**
     * Delete the {@link StoredSubmittable} related the {@link ValidationResult}.
     * Delete the {@link StoredSubmittable} related the {@link ProcessingStatus}.
     * Sends a message about {@link StoredSubmittable} deletion.
     * @param storedSubmittable the {@link StoredSubmittable} entity
     */
    @HandleAfterDelete
    public void handleAfterSubmittableDeletion(StoredSubmittable storedSubmittable) {
        deleteRelatedValidationResult(storedSubmittable);
        deleteRelatedProcessingStatus(storedSubmittable);
        sendStoredSubmittableDeletionMessage(storedSubmittable);
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

    private void deleteRelatedProcessingStatus(StoredSubmittable storedSubmittable) {
        ProcessingStatus processingStatus = storedSubmittable.getProcessingStatus();

        processingStatusRepository.delete(processingStatus);
    }

    private void sendStoredSubmittableDeletionMessage(StoredSubmittable storedSubmittable) {
        StoredSubmittableDeleteMessage storedSubmittableDeleteMessage = new StoredSubmittableDeleteMessage(
                storedSubmittable.getSubmission().getId()
        );

        rabbitMessagingTemplate.convertAndSend(
                Exchanges.SUBMISSIONS,
                STORED_SUBMITTABLE_DELETION_ROUTING_KEY,
                storedSubmittableDeleteMessage);
    }
}
