package uk.ac.ebi.subs.api.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.services.SubmissionStatusService;
import uk.ac.ebi.subs.data.status.StatusDescription;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.security.PreAuthorizeSubmissionIdTeamName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * This REST controller contains endpoints related to the status of a given submission.
 *
 * Created by karoly on 13/07/2017.
 */
@RequiredArgsConstructor
@RestController
@BasePathAwareController
@RequestMapping("/submissions/{submissionId}")
public class SubmissionStatusController {

    @NonNull
    private Map<String, StatusDescription> submissionStatusDescriptionMap;
    @NonNull
    private SubmissionRepository submissionRepository;
    @NonNull
    private SubmissionStatusService submissionStatusService;
    @NonNull
    private RepresentationModelAssembler<StatusDescription, EntityModel<StatusDescription>> submissionStatusResourceAssembler;
    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    @NonNull
    private ApplicationEventPublisher publisher;

    @NonNull
    private SubmissionStatusRepository submissionStatusRepository;

    /**
     * This endpoints could be used to update the status of a given submission.
     *
     * @param submissionId the Id of the submission
     * @param submissionStatusDto the payload data for update the status of the given submission
     * @return the updated submission status
     */
    @RequestMapping(value = "/submissionStatus",method = {RequestMethod.PUT,RequestMethod.PATCH})
    @PreAuthorizeSubmissionIdTeamName
    public EntityModel<SubmissionStatus> updateStatus(
            @PathVariable @P("submissionId") String submissionId,
            @RequestBody SubmissionStatusDto submissionStatusDto) {

        Submission submission = submissionRepository.findById(submissionId).orElse(null);

        if (submission == null) {
            throw new ResourceNotFoundException();
        }

        SubmissionStatus status = updateStatus(submissionStatusDto, submission);

        EntityModel<SubmissionStatus> resource = buildSubmissionStatusResource(status);

        return resource;
    }

    /**
     * Retrieves the status of a given submission.
     *
     * @param submissionId the Id of a submission
     * @return the status of the given submission.
     */
    @RequestMapping(value = "/submissionStatus",method = {RequestMethod.GET})
    @PreAuthorizeSubmissionIdTeamName
    public EntityModel<SubmissionStatus> getStatus(
            @PathVariable @P("submissionId") String submissionId) {

        Submission submission = submissionRepository.findById(submissionId).orElse(null);

        if (submission == null) {
            throw new ResourceNotFoundException();
        }

        SubmissionStatus status = submission.getSubmissionStatus();

        EntityModel<SubmissionStatus> resource = buildSubmissionStatusResource(status);

        return resource;
    }

    private EntityModel<SubmissionStatus> buildSubmissionStatusResource(SubmissionStatus status) {
        EntityModel<SubmissionStatus> resource = new EntityModel<>(status);

        resource.add(
                repositoryEntityLinks.linkToItemResource(status.getClass(), status.getId())
        );
        resource.add(
                repositoryEntityLinks.linkToItemResource(status.getClass(), status.getId()).withSelfRel()
        );

        return resource;
    }

    private SubmissionStatus updateStatus(SubmissionStatusDto submissionStatusDto, Submission submission) {
        SubmissionStatus status = submission.getSubmissionStatus();
        status.setStatus(submissionStatusDto.getStatus());

        publisher.publishEvent(new BeforeSaveEvent(status));
        status = submissionStatusRepository.save(status);
        publisher.publishEvent(new AfterSaveEvent(status));
        return status;
    }

    /**
     * Retrieves the available submission statuses.
     * The given submission's status could be transition to one of those statuses on the retrieved list.
     *
     * @param submissionId the Id of a submission
     * @return the available submission statuses the given submission's status could be transition to
     */
    @RequestMapping(value = "/availableSubmissionStatuses")
    @PreAuthorizeSubmissionIdTeamName
    public CollectionModel<EntityModel<StatusDescription>> availableSubmissionStatuses(@PathVariable String submissionId) {
        Submission currentSubmission = submissionRepository.findById(submissionId).orElse(null);

        Collection<String> statusNames =
                submissionStatusService.getAvailableStatusNames(currentSubmission, submissionStatusDescriptionMap);

        List<EntityModel<StatusDescription>> statusResources = statusNames
                .stream()
                .map(statusName -> submissionStatusDescriptionMap.get(statusName))
                .filter(Objects::nonNull)
                .map(statusDescription -> submissionStatusResourceAssembler.toModel(statusDescription))
                .collect(Collectors.toList());

        CollectionModel<EntityModel<StatusDescription>> resources = new CollectionModel<>(statusResources);

        resources.add(
                linkTo(
                        methodOn(this.getClass())
                                .availableSubmissionStatuses(submissionId)
                ).withSelfRel()
        );

        resources.add(
                repositoryEntityLinks.linkToItemResource(Submission.class, submissionId)
        );

        return resources;
    }
}
