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
import uk.ac.ebi.subs.api.controllers.SheetsController;
import uk.ac.ebi.subs.api.model.SubmissionContents;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionPlan;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.SpreadsheetRepository;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private OperationControlService operationControlService;
    @NonNull
    private ProjectRepository projectRepository;
    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    @NonNull
    private DataTypeRepository dataTypeRepository;

    @NonNull
    private List<Class<? extends StoredSubmittable>> submittablesClassList;

    private static final Logger logger = LoggerFactory.getLogger(SubmissionContentsProcessor.class);

    public Resource<SubmissionContents> process(Resource<SubmissionContents> resource) {

        String subId = resource.getContent().getSubmission().getId();

        List<DataType> dataTypesInSubmission = dataTypesInSubmission(resource.getContent().getSubmission());

        addSubmittablesInSubmission(dataTypesInSubmission, resource);
        addFilesLink(resource, subId);
        addProjectLink(resource, subId);
        addSpreadsheetLinks(resource, subId);

        if (operationControlService.isUpdateable(resource.getContent().getSubmission())) {
            addUpdateLinks(dataTypesInSubmission, resource);
            addSpreadsheetUploadLinks(resource, subId);
        }

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

    private void addSpreadsheetLinks(Resource<SubmissionContents> resource, String submissionId) {
        Map<String, String> templateExpansionParameters = paramWithSubmissionID(submissionId);
        templateExpansionParameters.put("dataTypeId", "samples");

        resource.add(createResourceLink(Spreadsheet.class, "by-submission-and-data-type",
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

    private void addSubmittablesInSubmission(List<DataType> dataTypesInSubmission, Resource<SubmissionContents> resource) {
        for (DataType dataType : dataTypesInSubmission) {

            Map<String, String> params = new HashMap<>();
            params.put("submissionId", resource.getContent().getSubmission().getId());
            params.put("dataTypeId", dataType.getId());

            Optional<Class<? extends StoredSubmittable>> optionalClass = submittablesClassList.stream()
                    .filter(clazz -> clazz.getName().equals(dataType.getSubmittableClassName()))
                    .findAny();

            if (optionalClass.isPresent()) {
                Link searchLink = repositoryEntityLinks.linkToSearchResource(optionalClass.get(), "by-submission-and-dataType");
                Link expandedLinks = searchLink.expand(params).withRel(dataType.getId());
                resource.getLinks().add(expandedLinks);
            } else {
                logger.error("Could not find class for data type {} in configured class list {}", dataType, submittablesClassList);
            }
        }
    }

    private void addUpdateLinks(List<DataType> dataTypesInSubmission, Resource<SubmissionContents> resource) {


        for (DataType dataType : dataTypesInSubmission) {
            linkHelper.addSubmittableCreateLink(
                    resource.getLinks(),
                    dataType,
                    resource.getContent().getSubmission());
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
