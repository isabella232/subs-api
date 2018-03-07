package uk.ac.ebi.subs.api.processors;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.controllers.SheetsController;
import uk.ac.ebi.subs.api.model.SubmissionContents;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

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

        addSubmittablesInSubmission(resource, subId);
        addFilesLink(resource, subId);
        addProjectLink(resource, subId);
        addSpreadsheetLinks(resource, subId);

        if (operationControlService.isUpdateable(resource.getContent().getSubmission())) {
            addUpdateLinks(resource);
            addSpreadsheetUploadLinks(resource, subId);
        }

        resource.getContent().setSubmission(null);

        return resource;
    }

    private void addSpreadsheetLinks(Resource<SubmissionContents> resource, String submissionId) {

        Map<String, String> templateExpansionParameters = paramWithSubmissionID(submissionId);
        templateExpansionParameters.put("templateTargetType", "samples");

        resource.add(createResourceLink(Sheet.class, "by-submission-and-target-type",
                templateExpansionParameters, "samplesSheets"));
    }

    private void addSpreadsheetUploadLinks(Resource<SubmissionContents> resource, String subId) {

        try {
            Link link =
                    linkTo(
                            methodOn(SheetsController.class)
                                    .uploadCsv(
                                            subId,
                                            null,//template name is required, must select one and use as param
                                            null
                                    )
                    ).withRel("sheetUpload");
            resource.add(link);
        } catch (IOException e) {
            //method is not actually invoked, so the exception can't occur
        }

    }

    private void addSubmittablesInSubmission(Resource<SubmissionContents> resource, String subId) {
        linkHelper.addSubmittablesInSubmissionLinks(
                resource.getLinks(),
                subId
        );
    }

    private void addUpdateLinks(Resource<SubmissionContents> resource) {
        linkHelper.addSubmittablesCreateLinks(resource.getContent().getSubmission(), resource.getLinks());

    }

    private void addProjectLink(Resource<SubmissionContents> resource, String submissionId) {
        if (projectRepository.findOneBySubmissionId(submissionId) != null) {
            resource.add(createResourceLink(Project.class, "project-by-submission",
                    paramWithSubmissionID(submissionId), "project"));
        }
    }

    private void addFilesLink(Resource<SubmissionContents> resource, String submissionId) {
        resource.add(createResourceLink(File.class, "by-submission",
                paramWithSubmissionID(submissionId), "files"));
    }

    private Map<String, String> paramWithSubmissionID(String submissionId) {
        Map<String, String> params = new HashMap<>();
        params.put("submissionId", submissionId);

        return params;
    }

    private Link createResourceLink(Class clazzResource, String rel, Map<String, String> params, String withRel) {
        return repositoryEntityLinks
                .linkToSearchResource(clazzResource, rel)
                .expand(params)
                .withRel(withRel);
    }
}
