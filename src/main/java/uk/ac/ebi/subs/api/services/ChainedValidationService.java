package uk.ac.ebi.subs.api.services;

import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class ChainedValidationService {

    private Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap;
    private SubmittableValidationDispatcher submittableValidationDispatcher;

    public ChainedValidationService(Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap, SubmittableValidationDispatcher submittableValidationDispatcher) {
        this.submittableValidationDispatcher = submittableValidationDispatcher;
        this.submittableRepositoryMap = submittableRepositoryMap;
    }

    public void triggerChainedValidation(StoredSubmittable storedSubmittable) {
        streamSubmittablesInSubmissionExceptTriggerSubmittable(storedSubmittable)
                .forEach(submittable -> submittableValidationDispatcher.validateUpdate(submittable));
    }

    public void triggerChainedValidation(Submission submission){
        streamSubmittablesInSubmission(submission.getId())
                .forEach(submittable -> submittableValidationDispatcher.validateUpdate(submittable));
    }

    protected Stream<? extends StoredSubmittable> streamSubmittablesInSubmission(String submissionId) {
        return submittableRepositoryMap.entrySet().stream()
                .map(entry -> entry.getValue())
                .flatMap( submittableRepository -> submittableRepository.streamBySubmissionId(submissionId));
    }

    protected Stream<? extends StoredSubmittable> streamSubmittablesInSubmissionExceptTriggerSubmittable(StoredSubmittable triggerSubmittable) {
        return streamSubmittablesInSubmission(triggerSubmittable.getSubmission().getId())
                .filter(submittable -> !submittable.getId().equals(triggerSubmittable.getId()));
    }

}
