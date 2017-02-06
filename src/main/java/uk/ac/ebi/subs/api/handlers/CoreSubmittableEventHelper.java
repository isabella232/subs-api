package uk.ac.ebi.subs.api.handlers;

import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.data.status.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;

import java.util.UUID;

@Component
@RepositoryEventHandler
public class CoreSubmittableEventHelper {

    /**
     * Give submittables an ID and draft status on creation
     *
     * @param submittable
     */
    @HandleBeforeCreate
    public void beforeCreate(StoredSubmittable submittable) {
        submittable.setId(UUID.randomUUID().toString());
        submittable.setStatus(ProcessingStatus.Draft);

        setDomainFromSubmission(submittable);
    }

    private void setDomainFromSubmission(StoredSubmittable submittable) {
        if (submittable.getSubmission() != null) {
            submittable.setDomain(submittable.getSubmission().getDomain());
        }
    }

    @HandleBeforeCreate
    public void beforeSave(StoredSubmittable storedSubmittable) {
        setDomainFromSubmission(storedSubmittable);
    }
}
