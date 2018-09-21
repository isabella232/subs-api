package uk.ac.ebi.subs.api.processors;

import lombok.Data;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.controllers.SubmissionContentsController;
import uk.ac.ebi.subs.api.model.SubmissionContents;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionPlan;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
@Data
public class SubmissionContentsProcessor implements ResourceProcessor<Resource<SubmissionContents>> {

    @NonNull
    private LinkHelper linkHelper;
    @NonNull
    private ProjectRepository projectRepository;
    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    @NonNull
    private DataTypeRepository dataTypeRepository;

    @NonNull
    private OperationControlService operationControlService;

    @NonNull
    private List<Class<? extends StoredSubmittable>> submittablesClassList;

    private static final Logger logger = LoggerFactory.getLogger(SubmissionContentsProcessor.class);

    public Resource<SubmissionContents> process(Resource<SubmissionContents> resource) {

        String subId = resource.getContent().getSubmission().getId();

        List<DataType> dataTypesInSubmission = dataTypesInSubmission(resource.getContent().getSubmission());

        addSubmittablesInSubmission(dataTypesInSubmission, resource);
        addFilesLink(resource, subId);
        addProjectLink(resource, subId);

        resource.getContent().setSubmission(null);

        return resource;
    }

    private List<DataType> dataTypesInSubmission(Submission submission) {
        SubmissionPlan submissionPlan = submission.getSubmissionPlan();


        List<DataType> dataTypesInSubmission;
        List<DataType> allDataTypes = dataTypeRepository.findAll();

        if (submissionPlan == null) {
            dataTypesInSubmission = allDataTypes;
        } else {
            Set<String> dataTypeIds = new HashSet<>(submissionPlan.getDataTypeIds());

            dataTypesInSubmission = allDataTypes.stream()
                    .filter(dt -> dataTypeIds.contains(dt.getId()))
                    .collect(Collectors.toList());
        }
        return dataTypesInSubmission;
    }


    private void addSubmittablesInSubmission(List<DataType> dataTypesInSubmission, Resource<SubmissionContents> resource) {
        boolean updateable = operationControlService.isUpdateable(resource.getContent().getSubmission());

        for (DataType dataType : dataTypesInSubmission) {

            Link collectionLink = linkTo(
                    methodOn(SubmissionContentsController.class)
                            .getSubmissionContents(
                                    resource.getContent().getSubmission().getId(),
                                    dataType.getId(),
                                    null,
                                    null
                            )
            ).withRel(dataType.getId());


            resource.add(collectionLink);

            if (updateable) {
                Link createLink = linkHelper.submittableCreateLink(dataType,resource.getContent().getSubmission());
                resource.add(createLink);
            }

        }
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
