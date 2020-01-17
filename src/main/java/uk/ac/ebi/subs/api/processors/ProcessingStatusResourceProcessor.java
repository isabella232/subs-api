package uk.ac.ebi.subs.api.processors;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.controllers.StatusDescriptionController;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * EntityModel processor for {@link ProcessingStatus} entity used by Spring MVC controller.
 */
@Component
public class ProcessingStatusResourceProcessor implements RepresentationModelProcessor<EntityModel<ProcessingStatus>> {

    @Override
    public EntityModel<ProcessingStatus> process(EntityModel<ProcessingStatus> resource) {

        addStatusDescriptionRel(resource);

        redactIds(resource);

        return resource;
    }

    private void redactIds(EntityModel<ProcessingStatus> resource) {
        resource.getContent().setSubmissionId(null);
        resource.getContent().setSubmittableId(null);
    }

    private void addStatusDescriptionRel(EntityModel<ProcessingStatus> resource) {
        resource.add(
                linkTo(
                        methodOn(StatusDescriptionController.class)
                                .processingStatus(resource.getContent().getStatus())
                ).withRel("statusDescription")
        );
    }
}
