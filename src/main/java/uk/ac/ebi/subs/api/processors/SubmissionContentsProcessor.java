package uk.ac.ebi.subs.api.processors;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.model.SubmissionContents;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;

import java.util.HashMap;
import java.util.Map;

@Component
@Data
public class SubmissionContentsProcessor implements ResourceProcessor<Resource<SubmissionContents>> {

    @NonNull
    private LinkHelper linkHelper;
    @NonNull
    private OperationControlService operationControlService;
    @NonNull
    private ProjectRepository projectRepository;
    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    public Resource<SubmissionContents> process(Resource<SubmissionContents> resource) {

        String subId = resource.getContent().getSubmission().getId();

        linkHelper.addSubmittablesInSubmissionLinks(
                resource.getLinks(),
                subId
        );

        if (projectRepository.findOneBySubmissionId(subId) != null) {
            Map<String,String> paramMap = new HashMap<>();
            paramMap.put("submissionId",subId);

            Link fetchLink = repositoryEntityLinks
                    .linkToSearchResource(Project.class, "project-by-submission")
                    .expand(paramMap)
                    .withRel("project");
            resource.add(fetchLink);
        }


        if (operationControlService.isUpdateable(resource.getContent().getSubmission())) {
            linkHelper.addSubmittablesCreateLinks(resource.getContent().getSubmission(), resource.getLinks());
        }

        resource.getContent().setSubmission(null);

        return resource;
    }

}
