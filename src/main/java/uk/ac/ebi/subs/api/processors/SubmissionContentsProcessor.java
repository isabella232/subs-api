package uk.ac.ebi.subs.api.processors;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.model.SubmissionContents;
import uk.ac.ebi.subs.api.services.OperationControlService;

@Component
public class SubmissionContentsProcessor implements ResourceProcessor<Resource<SubmissionContents>> {

    private LinkHelper linkHelper;
    private OperationControlService operationControlService;

    public Resource<SubmissionContents> process(Resource<SubmissionContents> resource) {

        linkHelper.addSubmittablesInSubmissionLinks(
                resource.getLinks(),
                resource.getContent().getSubmission().getId()
        );

        if (operationControlService.isUpdateable(resource.getContent().getSubmission())) {
            linkHelper.addSubmittablesCreateLinks(resource.getContent().getSubmission(), resource.getLinks());
        }

        resource.getContent().setSubmission(null);

        return resource;
    }

    public SubmissionContentsProcessor(LinkHelper linkHelper, OperationControlService operationControlService) {
        this.linkHelper = linkHelper;
        this.operationControlService = operationControlService;
    }
}
