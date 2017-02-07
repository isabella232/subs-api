package uk.ac.ebi.subs.api;

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
import uk.ac.ebi.subs.data.status.Status;

import java.util.List;
import java.util.Optional;

@RestController
@BasePathAwareController
public class StatusController {

    @Autowired
    private List<Status> releaseStatuses;

    @Autowired
    private List<Status> processingStatuses;

    @Autowired
    private List<Status> submissionStatuses;

    @Autowired
    private PagedResourcesAssembler pagedResourcesAssembler;

    @Autowired
    private ResourceAssembler<Status, Resource<Status>> processingStatusResourceAssembler;

    @Autowired
    private ResourceAssembler<Status, Resource<Status>> releaseStatusResourceAssembler;

    @Autowired
    private ResourceAssembler<Status, Resource<Status>> submissionStatusResourceAssembler;

    @RequestMapping("processingStatuses")
    public PagedResources<Resource<Status>> allProcessingStatus(Pageable pageable) {
        Page<Status> page = new PageImpl<Status>(processingStatuses, pageable, processingStatuses.size());

        return pagedResourcesAssembler.toResource(page, processingStatusResourceAssembler);
    }

    @RequestMapping("processingStatuses/{status}")
    public Resource<Status> processingStatus(@PathVariable String status) {
        Optional<Status> optionalStatus = processingStatuses.stream().filter(s -> s.getStatusName().equals(status))
                .findFirst();

        if (optionalStatus.isPresent()) {
            return processingStatusResourceAssembler.toResource(optionalStatus.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @RequestMapping("releaseStatuses")
    public PagedResources<Resource<Status>> allReleaseStatus(Pageable pageable) {
        Page<Status> page = new PageImpl<Status>(releaseStatuses, pageable, releaseStatuses.size());

        return pagedResourcesAssembler.toResource(page, releaseStatusResourceAssembler);
    }

    @RequestMapping("releaseStatuses/{status}")
    public Resource<Status> releaseStatus(@PathVariable String status) {
        Optional<Status> optionalStatus = releaseStatuses.stream().filter(s -> s.getStatusName().equals(status))
                .findFirst();

        if (optionalStatus.isPresent()) {
            return releaseStatusResourceAssembler.toResource(optionalStatus.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @RequestMapping("submissionStatuses")
    public PagedResources<Resource<Status>> allSubmissionStatus(Pageable pageable) {
        Page<Status> page = new PageImpl<Status>(submissionStatuses, pageable, submissionStatuses.size());

        return pagedResourcesAssembler.toResource(page, submissionStatusResourceAssembler);
    }

    @RequestMapping("submissionStatuses/{status}")
    public Resource<Status> submissionStatus(@PathVariable String status) {
        Optional<Status> optionalStatus = submissionStatuses.stream().filter(s -> s.getStatusName().equals(status))
                .findFirst();

        if (optionalStatus.isPresent()) {
            return submissionStatusResourceAssembler.toResource(optionalStatus.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }
}
