package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.model.SubmissionContents;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.security.PreAuthorizeSubmissionIdTeamName;

/**
 * This endpoint retrieves the list of links to create or retrieve the contents of a given submission.
 * The link will be processed by the SubmissionContentsProcessor class.
 *
 * Spring will call this class.
 */
@RestController
@BasePathAwareController
public class SubmissionContentsLinksController {

    private SubmissionRepository submissionRepository;

    public SubmissionContentsLinksController(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @PreAuthorizeSubmissionIdTeamName
    @RequestMapping("/submissions/{submissionId}/contents")
    public EntityModel<SubmissionContents> submissionContents(@PathVariable @P("submissionId") String submissionId) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);

        if (submission == null) {
            throw new ResourceNotFoundException();
        }

        EntityModel<SubmissionContents> submissionContentsResource = new EntityModel<>(
                new SubmissionContents(submission)
        );

        return submissionContentsResource;
    }

}
