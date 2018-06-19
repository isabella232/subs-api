package uk.ac.ebi.subs.api.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.Resources;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.processors.SubmissionStatusResourceProcessor;
import uk.ac.ebi.subs.api.services.FileService;
import uk.ac.ebi.subs.api.services.PersistentEntityCreationHelper;
import uk.ac.ebi.subs.api.services.SubmissionStatusService;
import uk.ac.ebi.subs.api.services.ValidationResultService;
import uk.ac.ebi.subs.data.status.StatusDescription;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.security.PreAuthorizeSubmissionIdTeamName;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
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
    private ValidationResultService validationResultService;
    @NonNull
    private SubmissionStatusService submissionStatusService;
    @NonNull
    private ResourceAssembler<StatusDescription, Resource<StatusDescription>> submissionStatusResourceAssembler;
    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    @NonNull
    private ApplicationEventPublisher publisher;

    @NonNull
    private SubmissionStatusRepository submissionStatusRepository;

    @NonNull
    private SubmissionStatusResourceProcessor submissionStatusResourceProcessor;


    @RequestMapping(value = "/submissionStatus",method = {RequestMethod.PUT,RequestMethod.PATCH})
    @PreAuthorizeSubmissionIdTeamName
    public Resource<SubmissionStatus> updateStatus(
            @PathVariable @P("submissionId") String submissionId,
            @RequestBody SubmissionStatusDto submissionStatusDto) {

        Submission submission = submissionRepository.findOne(submissionId);

        if (submission == null) {
            throw new ResourceNotFoundException();
        }

        SubmissionStatus status = updateStatus(submissionStatusDto, submission);

        Resource<SubmissionStatus> resource = buildSubmissionStatusResource(status);

        return resource;
    }

    private Resource<SubmissionStatus> buildSubmissionStatusResource(SubmissionStatus status) {
        Resource<SubmissionStatus> resource = new Resource<>(status);

        resource.add(
                repositoryEntityLinks.linkToSingleResource(status)
        );
        resource.add(
                repositoryEntityLinks.linkToSingleResource(status).withSelfRel()
        );

        resource = submissionStatusResourceProcessor.process(resource);
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

    @RequestMapping(value = "/availableSubmissionStatuses")
    @PreAuthorizeSubmissionIdTeamName
    public Resources<Resource<StatusDescription>> availableSubmissionStatuses(@PathVariable String submissionId) {
        Submission currentSubmission = submissionRepository.findOne(submissionId);

        Collection<String> statusNames =
                submissionStatusService.getAvailableStatusNames(currentSubmission, submissionStatusDescriptionMap);

        List<Resource<StatusDescription>> statusResources = statusNames
                .stream()
                .map(statusName -> submissionStatusDescriptionMap.get(statusName))
                .filter(Objects::nonNull)
                .map(statusDescription -> submissionStatusResourceAssembler.toResource(statusDescription))
                .collect(Collectors.toList());

        Resources<Resource<StatusDescription>> resources = new Resources<>(statusResources);

        resources.add(
                linkTo(
                        methodOn(this.getClass())
                                .availableSubmissionStatuses(submissionId)
                ).withSelfRel()
        );

        resources.add(
                repositoryEntityLinks.linkToSingleResource(Submission.class, submissionId)
        );


        return resources;
    }
}
