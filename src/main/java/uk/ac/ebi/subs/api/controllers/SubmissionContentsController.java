package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
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
public class SubmissionContentsController {

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
    public Resource<SubmissionContents> submissionContents(@PathVariable @P("submissionId") String submissionId) {
        Submission submission = submissionRepository.findOne(submissionId);

        if (submission == null) {
            throw new ResourceNotFoundException();
        }
        return this.process(new Resource<>(new SubmissionContents(submission)));
    }

    public Resource<SubmissionContents> process(Resource<SubmissionContents> resource) {
        linkHelper.addSubmittablesInSubmissionLinks(resource.getLinks(), resource.getContent().getSubmission().getId());

        if (operationControlService.isUpdateable(resource.getContent().getSubmission() )) {
            linkHelper.addSubmittablesCreateLinks(resource.getLinks());
        }

        resource.getContent().setSubmission(null);

        return resource;
    }


    public class SubmissionContents {
        private Submission submission;

        public SubmissionContents(Submission submission) {
            this.submission = submission;
        }

        public Submission getSubmission() {
            return submission;
        }

        public void setSubmission(Submission submission) {
            this.submission = submission;
        }
    }
}
