package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.data.status.StatusDescription;

import java.util.List;
import java.util.Optional;

/**
 * These are REST endpoints describing the various statuses (processingStatus, releaseStatus, submissionStatus) descriptions,
 * transitions (user, system) and whether it accepts updates from the user.
 *
 * TODO: remove the release status related endpoints
 */
@RestController
@BasePathAwareController
@RequestMapping("/statusDescriptions")
public class StatusDescriptionController {


    private List<StatusDescription> releaseStatuses;
    private List<StatusDescription> processingStatuses;
    private List<StatusDescription> submissionStatuses;
    private PagedResourcesAssembler pagedResourcesAssembler;
    private RepresentationModelAssembler<StatusDescription, EntityModel<StatusDescription>> processingStatusResourceAssembler;
    private RepresentationModelAssembler<StatusDescription, EntityModel<StatusDescription>> releaseStatusResourceAssembler;
    private RepresentationModelAssembler<StatusDescription, EntityModel<StatusDescription>> submissionStatusResourceAssembler;
    public StatusDescriptionController(List<StatusDescription> releaseStatuses, List<StatusDescription> processingStatuses, List<StatusDescription> submissionStatuses, PagedResourcesAssembler pagedResourcesAssembler, RepresentationModelAssembler<StatusDescription, EntityModel<StatusDescription>> processingStatusResourceAssembler, RepresentationModelAssembler<StatusDescription, EntityModel<StatusDescription>> releaseStatusResourceAssembler, RepresentationModelAssembler<StatusDescription, EntityModel<StatusDescription>> submissionStatusResourceAssembler) {
        this.releaseStatuses = releaseStatuses;
        this.processingStatuses = processingStatuses;
        this.submissionStatuses = submissionStatuses;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.processingStatusResourceAssembler = processingStatusResourceAssembler;
        this.releaseStatusResourceAssembler = releaseStatusResourceAssembler;
        this.submissionStatusResourceAssembler = submissionStatusResourceAssembler;
    }

    @RequestMapping("/processingStatuses")
    public PagedModel<EntityModel<StatusDescription>> allProcessingStatus(Pageable pageable) {
        Page<StatusDescription> page = new PageImpl<>(processingStatuses, pageable, processingStatuses.size());

        return pagedResourcesAssembler.toModel(page, processingStatusResourceAssembler);
    }

    @RequestMapping("/processingStatuses/{status}")
    public EntityModel<StatusDescription> processingStatus(@PathVariable String status) {
        Optional<StatusDescription> optionalStatus = processingStatuses.stream().filter(s -> s.getStatusName().equals(status))
                .findFirst();

        if (optionalStatus.isPresent()) {
            return processingStatusResourceAssembler.toModel(optionalStatus.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @RequestMapping("/releaseStatuses")
    public PagedModel<EntityModel<StatusDescription>> allReleaseStatus(Pageable pageable) {
        Page<StatusDescription> page = new PageImpl<StatusDescription>(releaseStatuses, pageable, releaseStatuses.size());

        return pagedResourcesAssembler.toModel(page, releaseStatusResourceAssembler);
    }

    @RequestMapping("/releaseStatuses/{status}")
    public EntityModel<StatusDescription> releaseStatus(@PathVariable String status) {
        Optional<StatusDescription> optionalStatus = releaseStatuses.stream().filter(s -> s.getStatusName().equals(status))
                .findFirst();

        if (optionalStatus.isPresent()) {
            return releaseStatusResourceAssembler.toModel(optionalStatus.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @RequestMapping("/submissionStatuses")
    public PagedModel<EntityModel<StatusDescription>> allSubmissionStatus(Pageable pageable) {
        Page<StatusDescription> page = new PageImpl<StatusDescription>(submissionStatuses, pageable, submissionStatuses.size());

        return pagedResourcesAssembler.toModel(page, submissionStatusResourceAssembler);
    }

    @RequestMapping("submissionStatuses/{status}")
    public EntityModel<StatusDescription> submissionStatus(@PathVariable String status) {
        Optional<StatusDescription> optionalStatus = submissionStatuses.stream().filter(s -> s.getStatusName().equals(status))
                .findFirst();

        if (optionalStatus.isPresent()) {
            return submissionStatusResourceAssembler.toModel(optionalStatus.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }
}
