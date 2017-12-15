package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ebi.subs.api.services.PersistentEntityCreationHelper;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.security.PreAuthorizeSubmissionIdTeamName;

@RepositoryRestController
public class SubmissionContentsController {

    private SubmissionRepository submissionRepository;
    private PersistentEntityCreationHelper persistentEntityCreationHelper;

    public SubmissionContentsController(SubmissionRepository submissionRepository, PersistentEntityCreationHelper persistentEntityCreationHelper) {
        this.submissionRepository = submissionRepository;
        this.persistentEntityCreationHelper = persistentEntityCreationHelper;
    }

    @PreAuthorizeSubmissionIdTeamName
    @RequestMapping(value = "/submissions/{submissionId}/contents/{repository}", method = RequestMethod.POST)
    public ResponseEntity<ResourceSupport> createSubmissionContents(
            @PathVariable @P("submissionId") String submissionId,
            PersistentEntityResource payload,
            PersistentEntityResourceAssembler assembler,
            RootResourceInformation resourceInformation,
            @RequestHeader(value = "Accept", required = false) String acceptHeader
    ) {
        Submission submission = submissionRepository.findOne(submissionId);

        if (submission == null) {
            throw new ResourceNotFoundException();
        }

        if (!StoredSubmittable.class.isAssignableFrom(payload.getContent().getClass())) {
            throw new IllegalArgumentException();
        }

        StoredSubmittable submittable = (StoredSubmittable) payload.getContent();
        submittable.setSubmission(submission);

        return persistentEntityCreationHelper.createPersistentEntity(
                payload.getContent(),
                resourceInformation,
                assembler,
                acceptHeader
        );
    }


}
