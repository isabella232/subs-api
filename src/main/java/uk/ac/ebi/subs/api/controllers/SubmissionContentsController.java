package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.processors.LinkHelper;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.security.PreAuthorizeSubmissionIdTeamName;

@RestController
@BasePathAwareController
public class SubmissionContentsController implements ResourceProcessor<SubmissionContentsController.SubmissionContentsResource> {

    private SubmissionRepository submissionRepository;
    private LinkHelper linkHelper;
    private OperationControlService operationControlService;

    public SubmissionContentsController(SubmissionRepository submissionRepository, LinkHelper linkHelper, OperationControlService operationControlService) {
        this.submissionRepository = submissionRepository;
        this.linkHelper = linkHelper;
        this.operationControlService = operationControlService;
    }

    @PreAuthorizeSubmissionIdTeamName
    @RequestMapping("/submissions/{submissionId}/contents")
    public SubmissionContentsResource submissionContents(@PathVariable @P("submissionId") String submissionId) {
        Submission submission = submissionRepository.findOne(submissionId);

        if (submission == null) {
            throw new ResourceNotFoundException();
        }

        return new SubmissionContentsResource(submission);

    }

    @Override
    public SubmissionContentsResource process(SubmissionContentsResource resource) {
        linkHelper.addSubmittablesInSubmissionLinks(resource.getLinks(), resource.submission.getId());

        if (operationControlService.isUpdateable(resource.submission)) {
            linkHelper.addSubmittablesCreateLinks(resource.getLinks());
        }

        resource.submission = null;

        return resource;
    }

    class SubmissionContentsResource extends ResourceSupport {
        private Submission submission;

        public SubmissionContentsResource(Submission submission) {
            this.submission = submission;
        }
    }
}
