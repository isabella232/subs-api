package uk.ac.ebi.subs.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.data.status.StatusDescription;

import java.util.List;
import java.util.Optional;

@RestController
@BasePathAwareController
@RequestMapping("/statusDescriptions")
public class StatusDescriptionController {

    @Autowired
    private List<StatusDescription> releaseStatuses;

    @Autowired
    private List<StatusDescription> processingStatuses;

    @Autowired
    private List<StatusDescription> submissionStatuses;

    @Autowired
    private PagedResourcesAssembler pagedResourcesAssembler;

    @Autowired
    private ResourceAssembler<StatusDescription, Resource<StatusDescription>> processingStatusResourceAssembler;

    @Autowired
    private ResourceAssembler<StatusDescription, Resource<StatusDescription>> releaseStatusResourceAssembler;

    @Autowired
    private ResourceAssembler<StatusDescription, Resource<StatusDescription>> submissionStatusResourceAssembler;

    @RequestMapping("/processingStatuses")
    public PagedResources<Resource<StatusDescription>> allProcessingStatus(Pageable pageable) {
        Page<StatusDescription> page = new PageImpl<StatusDescription>(processingStatuses, pageable, processingStatuses.size());

        return pagedResourcesAssembler.toResource(page, processingStatusResourceAssembler);
    }

    @RequestMapping("/processingStatuses/{status}")
    public Resource<StatusDescription> processingStatus(@PathVariable String status) {
        Optional<StatusDescription> optionalStatus = processingStatuses.stream().filter(s -> s.getStatusName().equals(status))
                .findFirst();

        if (optionalStatus.isPresent()) {
            return processingStatusResourceAssembler.toResource(optionalStatus.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @RequestMapping("/releaseStatuses")
    public PagedResources<Resource<StatusDescription>> allReleaseStatus(Pageable pageable) {
        Page<StatusDescription> page = new PageImpl<StatusDescription>(releaseStatuses, pageable, releaseStatuses.size());

        return pagedResourcesAssembler.toResource(page, releaseStatusResourceAssembler);
    }

    @RequestMapping("/releaseStatuses/{status}")
    public Resource<StatusDescription> releaseStatus(@PathVariable String status) {
        Optional<StatusDescription> optionalStatus = releaseStatuses.stream().filter(s -> s.getStatusName().equals(status))
                .findFirst();

        if (optionalStatus.isPresent()) {
            return releaseStatusResourceAssembler.toResource(optionalStatus.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @RequestMapping("/submissionStatuses")
    public PagedResources<Resource<StatusDescription>> allSubmissionStatus(Pageable pageable) {
        Page<StatusDescription> page = new PageImpl<StatusDescription>(submissionStatuses, pageable, submissionStatuses.size());

        return pagedResourcesAssembler.toResource(page, submissionStatusResourceAssembler);
    }

    @RequestMapping("submissionStatuses/{status}")
    public Resource<StatusDescription> submissionStatus(@PathVariable String status) {
        Optional<StatusDescription> optionalStatus = submissionStatuses.stream().filter(s -> s.getStatusName().equals(status))
                .findFirst();

        if (optionalStatus.isPresent()) {
            return submissionStatusResourceAssembler.toResource(optionalStatus.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }
}
