package uk.ac.ebi.subs.api.sheetloader;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.validation.ObjectError;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SheetBulkOps {

    @NonNull
    private ValidationResultRepository validationResultRepository;

    @NonNull
    private ProcessingStatusRepository processingStatusRepository;

    @NonNull
    private ApplicationEventPublisher applicationEventPublisher;

    public Collection<Pair<Row, ? extends StoredSubmittable>> lookupExistingEntries(Submission submission, Collection<Pair<Row, ? extends StoredSubmittable>> submittables, SubmittableRepository<?> repository) {
        Map<String, StoredSubmittable> submittablesByAlias = new HashMap<>();


        for (Pair<Row, ? extends StoredSubmittable> pair : submittables) {
            StoredSubmittable s = pair.getSecond();
            if (s.getAlias() != null) {
                submittablesByAlias.put(s.getAlias(), s);
            }
        }
        List<? extends StoredSubmittable> dbSubmittables = repository.findBySubmissionIdAndAliasIn(
                submission.getId(),
                submittablesByAlias.keySet()
        );


        for (StoredSubmittable dbSubmittable : dbSubmittables) {
            if (dbSubmittable.getAlias() != null && submittablesByAlias.containsKey(dbSubmittable.getAlias())) {
                StoredSubmittable sheetSubmittable = submittablesByAlias.get(dbSubmittable.getAlias());

                sheetSubmittable.setId(dbSubmittable.getId());
                sheetSubmittable.setVersion(dbSubmittable.getVersion());
                sheetSubmittable.setCreatedBy(dbSubmittable.getCreatedBy());
                sheetSubmittable.setCreatedDate(dbSubmittable.getCreatedDate());
            }

        }

        return submittables;
    }

    public void updateExistingSubmittables(
            Collection<Pair<Row, ? extends StoredSubmittable>> existingSubmittables,
            SubmittableRepository repository) {

        Collection<StoredSubmittable> submittablesToSave = new LinkedList<>();
        for (Pair<Row, ? extends StoredSubmittable> pair : existingSubmittables) {
            pair.getFirst().setProcessed(true);
            submittablesToSave.add(pair.getSecond());
        }

        repository.save(submittablesToSave);
    }

    private void addErrorToRow(RepositoryConstraintViolationException exception, Row row) {
        for (ObjectError objectError : exception.getErrors().getAllErrors()) {
            row.getErrors().add(objectError.getDefaultMessage());
        }
    }

    public void insertNewSubmittables(Collection<Pair<Row, ? extends StoredSubmittable>> freshSubmittables,
                                      SubmittableRepository repository) {

        Collection<ValidationResult> validationResults = new ArrayList<>();
        Collection<ProcessingStatus> processingStatuses = new ArrayList<>();

        Collection<StoredSubmittable> submittablesToSave = new ArrayList<>();

        for (Pair<Row, ? extends StoredSubmittable> pair : freshSubmittables) {
            Row row = pair.getFirst();
            StoredSubmittable storedSubmittable = pair.getSecond();

            submittablesToSave.add(storedSubmittable);
            validationResults.add(validationResult(storedSubmittable));
            processingStatuses.add(processingStatus(storedSubmittable));

            row.setProcessed(true);
        }

        validationResultRepository.insert(validationResults);
        processingStatusRepository.insert(processingStatuses);
        repository.insert(submittablesToSave);
    }

    private ProcessingStatus processingStatus(StoredSubmittable storedSubmittable) {
        ProcessingStatus processingStatus = ProcessingStatus.createForSubmittable(storedSubmittable);
        processingStatus.setId(UUID.randomUUID().toString());

        return processingStatus;
    }

    private ValidationResult validationResult(StoredSubmittable storedSubmittable) {
        ValidationResult validationResult = new ValidationResult();
        validationResult.setEntityUuid(storedSubmittable.getId());
        validationResult.setUuid(UUID.randomUUID().toString());

        validationResult.setSubmissionId(storedSubmittable.getSubmission().getId());

        storedSubmittable.setValidationResult(validationResult);

        return validationResult;
    }
}
