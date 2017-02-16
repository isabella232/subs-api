package uk.ac.ebi.subs.api.resourceAssembly;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.StatusDescriptionController;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class RootEndpointLinkProcessor implements ResourceProcessor<RepositoryLinksResource> {

    private Pageable defaultPageRequest() {
        return new PageRequest(0, 1);
    }

    public RootEndpointLinkProcessor() {


    }


    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {

        Pageable pageable = defaultPageRequest();

        resource.add(
                linkTo(
                        methodOn(StatusDescriptionController.class)
                                .allProcessingStatus(pageable))
                        .withRel("processingStatusDescriptions")
        );
        resource.add(
                linkTo(
                        methodOn(StatusDescriptionController.class)
                                .allReleaseStatus(pageable))
                        .withRel("releaseStatusDescriptions")
        );
        resource.add(
                linkTo(
                        methodOn(StatusDescriptionController.class)
                                .allSubmissionStatus(pageable))
                        .withRel("submissionStatusDescriptions")
        );

        return resource;
    }
}

