package uk.ac.ebi.subs.api.processors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.controllers.StatusDescriptionController;
import uk.ac.ebi.subs.api.controllers.SubmissionStatusController;
import uk.ac.ebi.subs.api.services.SubmissionStatusService;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * EntityModel processor for {@link SubmissionStatus} entity used by Spring MVC controller.
 */
@Component
@RequiredArgsConstructor
public class SubmissionStatusResourceProcessor implements RepresentationModelProcessor<EntityModel<SubmissionStatus>> {

    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    @NonNull
    private SubmissionRepository submissionRepository;

    @NonNull
    private SubmissionStatusService submissionStatusService;

    public static final String AVAILABLE_STATUSES_REL = "availableStatuses";
    public static final String STATUS_REL = "submissionStatus";

    @Override
    public EntityModel<SubmissionStatus> process(EntityModel<SubmissionStatus> resource) {

        addStatusDescriptionRel(resource);

        addStatusUpdateRel(resource);

        addAvailableStatuses(resource);

        return resource;
    }

    private void addStatusDescriptionRel(EntityModel<SubmissionStatus> resource) {
        resource.add(
            linkTo(
                    methodOn(StatusDescriptionController.class)
                            .submissionStatus(resource.getContent().getStatus())
            ).withRel("statusDescription")
        );
    }

    private void addStatusUpdateRel(EntityModel<SubmissionStatus> submissionStatusResource) {
        SubmissionStatus submissionStatus = submissionStatusResource.getContent();

        if (submissionStatusService.isSubmissionStatusChangeable(submissionStatus)) {
            Link submissionStatusResourceLink = repositoryEntityLinks.linkToItemResource(
                    submissionStatus.getClass(), submissionStatus.getId()).expand();

            Assert.notNull(submissionStatusResourceLink);

            Link updateLink = submissionStatusResourceLink.withRel("self" + LinkHelper.UPDATE_REL_SUFFIX);
            if (submissionStatusResource.getLink(updateLink.getRel()).isEmpty()) {
                submissionStatusResource.add(updateLink);
            }
        }
    }

    private void addAvailableStatuses(EntityModel<SubmissionStatus> submissionStatusResource) {
        SubmissionStatus submissionStatus = submissionStatusResource.getContent();
        Submission submission = submissionRepository.findBySubmissionStatusId(submissionStatus.getId());

        submissionStatusResource.add(
                linkTo(
                        methodOn(SubmissionStatusController.class)
                                .availableSubmissionStatuses(submission.getId())
                ).withRel(AVAILABLE_STATUSES_REL)
        );
    }
}
