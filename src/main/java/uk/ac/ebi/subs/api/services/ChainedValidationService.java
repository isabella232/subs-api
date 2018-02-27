package uk.ac.ebi.subs.api.services;

import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChainedValidationService {

    private Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap;
    private SubmittableValidationDispatcher submittableValidationDispatcher;

    public ChainedValidationService(Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap, SubmittableValidationDispatcher submittableValidationDispatcher) {
        this.submittableValidationDispatcher = submittableValidationDispatcher;
        this.submittableRepositoryMap = submittableRepositoryMap;
    }

    public void triggerChainedValidation(StoredSubmittable storedSubmittable) {
        Map<String, List<? extends StoredSubmittable>> submittablesInSubmission = findSubmittablesInSubmission(storedSubmittable.getSubmission().getId());

        filterOutTriggerSubmittable(storedSubmittable, submittablesInSubmission);

        submittablesInSubmission.entrySet().forEach(es -> {
            for (StoredSubmittable submittable : es.getValue()) {
                submittableValidationDispatcher.validateUpdate(submittable);
            }
        });
    }

    public Map<String, List<? extends StoredSubmittable>> findSubmittablesInSubmission(String submissionId) {
        Map<String, List<? extends StoredSubmittable>> submittablesInSubmission = new HashMap<>();

        this.submittableRepositoryMap.entrySet().forEach(es ->
            submittablesInSubmission.put(es.getKey().getSimpleName(), es.getValue().findBySubmissionId(submissionId))
        );

        return submittablesInSubmission;
    }

    public void filterOutTriggerSubmittable(StoredSubmittable storedSubmittable, Map<String, List<? extends StoredSubmittable>> submittablesInSubmission) {
        submittablesInSubmission.entrySet().forEach(es ->
            es.getValue().removeIf(ss -> ss.getId().equals(storedSubmittable.getId()))
        );
    }

}
