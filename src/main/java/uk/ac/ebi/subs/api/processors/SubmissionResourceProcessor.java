package uk.ac.ebi.subs.api.processors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.core.ControllerEntityLinks;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.controllers.ProcessingStatusController;
import uk.ac.ebi.subs.api.controllers.SubmissionBlockersSummaryController;
import uk.ac.ebi.subs.api.controllers.SubmissionContentsLinksController;
import uk.ac.ebi.subs.api.controllers.SubmissionStatusController;
import uk.ac.ebi.subs.api.controllers.TeamController;
import uk.ac.ebi.subs.api.model.SubmissionResource;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.api.services.SubmissionStatusService;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.validator.data.ValidationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * EntityModel processor for {@link Submission} entity used by Spring MVC controller.
 */
@Component
@RequiredArgsConstructor
public class SubmissionResourceProcessor implements RepresentationModelProcessor<EntityModel<Submission>> {

    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    @NonNull
    private OperationControlService operationControlService;

    @NonNull
    private LinkHelper linkHelper;

    @NonNull
    private SubmissionStatusService submissionStatusService;

    @NonNull
    private ControllerEntityLinks controllerEntityLinks;

    @NonNull
    private DataTypeRepository dataTypeRepository;

    @Override
    public EntityModel<Submission> process(EntityModel<Submission> resource) {
        addTeamRel(resource);
        addContentsRels(resource);
        addValidationResultLinks(resource);

        ifUpdateableAddLinks(resource);

        addStatusLinks(resource);

        addStatusSummaryReport(resource);
        addSubmissionBlockersSummary(resource);
        addTypeStatusSummaryReport(resource);

        addReceiptLink(resource);

        SubmissionResource submissionResource = new SubmissionResource(resource);

        addDataType(submissionResource);

        //submissionResource.getContent().setSubmissionStatus(null);

        return submissionResource;
    }

    private void addDataType(SubmissionResource resource) {
        Submission submission = resource.getContent();

        if (submission.getSubmissionPlan() == null){
            return;
        }

        List<DataType> dataTypes = submission.getSubmissionPlan().getDataTypeIds()
                .stream()
                .map(id -> dataTypeRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        resource.setDataTypes(dataTypes);
    }

    private void addStatusLinks(EntityModel<Submission> resource) {
        Submission submission = resource.getContent();

        if (submissionStatusService.isSubmissionStatusChangeable(submission)) {
            Link updateLink = linkTo(
                    methodOn(SubmissionStatusController.class)
                            .updateStatus(submission.getId(),null)
                    ).withRel("submissionStatus"+LinkHelper.UPDATE_REL_SUFFIX);

            resource.add(updateLink);
        }

        if (resource.getLink(SubmissionStatusResourceProcessor.STATUS_REL).isEmpty()) {
            Link statusLink = linkTo(
                    methodOn(SubmissionStatusController.class)
                            .getStatus(submission.getId())
            ).withRel(SubmissionStatusResourceProcessor.STATUS_REL);

            resource.add(statusLink);
        }

        Link availableStatusLinks = linkTo(
                methodOn(SubmissionStatusController.class)
                        .availableSubmissionStatuses(submission.getId())
        ).withRel(SubmissionStatusResourceProcessor.AVAILABLE_STATUSES_REL);

        resource.add(availableStatusLinks);

    }

    private void addReceiptLink(EntityModel<Submission> resource) {
        Link searchLink = repositoryEntityLinks.linkToSearchResource(ProcessingStatus.class, LinkRelation.of("by-submission"));
        Assert.notNull(searchLink);

        Map<String, String> params = new HashMap<>();
        params.put("submissionId", resource.getContent().getId());

        Link link = searchLink.expand(params).withRel("processingStatuses");

        resource.add(link);
    }

    private void addStatusSummaryReport(EntityModel<Submission> resource) {
        Link statusSummary =
                linkTo(
                        methodOn(ProcessingStatusController.class)
                                .summariseProcessingStatusForSubmission(resource.getContent().getId())

                ).withRel("processingStatusSummary");


        resource.add(statusSummary);
    }

    private void addSubmissionBlockersSummary(EntityModel<Submission> resource) {
        Link blockersSummary =
                linkTo(
                        methodOn(SubmissionBlockersSummaryController.class)
                                .getSubmissionContentsIssuesSummary(resource.getContent().getId())
                ).withRel("submissionBlockersSummary");

        resource.add(blockersSummary);
    }

    private void addTypeStatusSummaryReport(EntityModel<Submission> resource) {
        Link typeStatusSummary =
                linkTo(
                        methodOn(ProcessingStatusController.class)
                                .summariseTypeProcessingStatusForSubmission(resource.getContent().getId())
                ).withRel("typeProcessingStatusSummary");


        resource.add(typeStatusSummary);
    }

    private void ifUpdateableAddLinks(EntityModel<Submission> submissionResource) {
        if (operationControlService.isUpdateable(submissionResource.getContent())) {

            submissionResource.add(
                    linkHelper.addSelfUpdateLink(new ArrayList<>(), submissionResource.getContent())
            );
        }
    }

    private void addContentsRels(EntityModel<Submission> submissionResource) {

        submissionResource.add(
            linkTo(
                    methodOn(SubmissionContentsLinksController.class)
                            .submissionContents(submissionResource.getContent().getId())
            ).withRel("contents")
        );
    }

    private void addValidationResultLinks(EntityModel<Submission> submissionResource) {
        Map<String, String> expansionParams = new HashMap<>();
        expansionParams.put("submissionId", submissionResource.getContent().getId());

        addObjectToSubmissionAsLink(ValidationResult.class, submissionResource, expansionParams);
    }

    private void addObjectToSubmissionAsLink(Class<?> linkToBeAdded, EntityModel<Submission> submissionResource, Map<String, String> expansionParams) {
        Link contentsLink = repositoryEntityLinks.linkToSearchResource(linkToBeAdded, LinkRelation.of("by-submission"));
        Link collectionLink = repositoryEntityLinks.linkToCollectionResource(linkToBeAdded);

        Assert.notNull(contentsLink);
        Assert.notNull(collectionLink);

        submissionResource.add(
                contentsLink.expand(expansionParams).withRel(collectionLink.getRel())
        );
    }

    private void addTeamRel(EntityModel<Submission> resource) {
        if (resource.getContent().getTeam() != null && resource.getContent().getTeam().getName() != null) {
            resource.add(
                    linkTo(
                            methodOn(TeamController.class)
                                    .getTeam(resource.getContent().getTeam().getName())
                    ).withRel("team")
            );
        }
    }
}
