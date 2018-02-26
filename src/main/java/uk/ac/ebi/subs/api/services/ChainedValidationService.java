package uk.ac.ebi.subs.api.services;

import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChainedValidationService {

    private Map<String, SubmittableRepository> submittableRepositoryMap;
    private SubmittableValidationDispatcher submittableValidationDispatcher;

    public ChainedValidationService(Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap, SubmittableValidationDispatcher submittableValidationDispatcher) {
        buildSubmittableRepositoryMap(submittableRepositoryMap);
        this.submittableValidationDispatcher = submittableValidationDispatcher;
    }

    public void triggerChainedValidation(StoredSubmittable storedSubmittable) {
        Map<String, List<? extends StoredSubmittable>> submittablesInSubmission = findSubmittablesInSubmission(storedSubmittable.getSubmission().getId());

        // TODO - Use submittableId to filter out submittable that triggered validation on creation

        submittablesInSubmission.entrySet().forEach(es -> {
            for (StoredSubmittable submittable : es.getValue()) {
                submittableValidationDispatcher.validateUpdate(submittable);
            }
        });
    }

    private Map<String, List<? extends StoredSubmittable>> findSubmittablesInSubmission(String submissionId) {
        Map<String, List<? extends StoredSubmittable>> submittablesInSubmission = new HashMap<>();

        this.submittableRepositoryMap.entrySet().forEach(es ->
            submittablesInSubmission.put(es.getKey(), es.getValue().findBySubmissionId(submissionId))
        );

        return submittablesInSubmission;
    }

    private void buildSubmittableRepositoryMap(Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap) {
        this.submittableRepositoryMap = new HashMap<>();
        submittableRepositoryMap.entrySet().forEach(es ->
                this.submittableRepositoryMap.put(es.getKey().getSimpleName(), es.getValue())
        );
    }
}
