package uk.ac.ebi.subs.api.handlers;

import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.services.SubmissionEventService;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.SubmissionRepository;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;

/**
 * Created by davidr on 20/02/2017.
 */
@Component
@RepositoryEventHandler(SubmissionStatus.class)
public class SubmissionStatusEventHandler {

    private SubmissionRepository submissionRepository;
    private SubmissionEventService submissionEventService;

    public SubmissionStatusEventHandler(SubmissionRepository submissionRepository){
        this.submissionRepository = submissionRepository;
    }

    @HandleBeforeSave
    public void handleUpdate(SubmissionStatus submissionStatus){
        if (SubmissionStatusEnum.Submitted.name().equals(submissionStatus.getStatus())){

        }
    }
}
