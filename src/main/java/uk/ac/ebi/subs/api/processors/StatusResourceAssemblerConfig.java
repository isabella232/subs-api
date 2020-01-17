package uk.ac.ebi.subs.api.processors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import uk.ac.ebi.subs.api.controllers.StatusDescriptionController;
import uk.ac.ebi.subs.data.status.StatusDescription;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * This configuration class contains resource assembler Spring beans for
 * {@link uk.ac.ebi.subs.repository.model.SubmissionStatus}, {@link uk.ac.ebi.subs.repository.model.ProcessingStatus}
 * and release status.
 *
 * TODO: remove release status. Not used.
 */
@Configuration
public class StatusResourceAssemblerConfig {

    @Bean
    public RepresentationModelAssembler<StatusDescription, EntityModel<StatusDescription>> submissionStatusResourceAssembler() {
        return entity -> {
            EntityModel<StatusDescription> res = new EntityModel<StatusDescription>(entity);

            res.add(
                    linkTo(
                            methodOn(StatusDescriptionController.class).submissionStatus(entity.getStatusName()
                            )
                    ).withSelfRel()
            );

            return res;
        };
    }

    @Bean
    public RepresentationModelAssembler<StatusDescription, EntityModel<StatusDescription>> processingStatusResourceAssembler() {
        return entity -> {
            EntityModel<StatusDescription> res = new EntityModel<StatusDescription>(entity);

            res.add(
                    linkTo(
                            methodOn(StatusDescriptionController.class).processingStatus(entity.getStatusName())
                    ).withSelfRel()
            );

            return res;
        };
    }

    @Bean
    public RepresentationModelAssembler<StatusDescription, EntityModel<StatusDescription>> releaseStatusResourceAssembler() {
        return entity -> {
            EntityModel<StatusDescription> res = new EntityModel<StatusDescription>(entity);

            res.add(
                    linkTo(
                            methodOn(StatusDescriptionController.class).releaseStatus(entity.getStatusName())
                    ).withSelfRel()
            );

            return res;
        };
    }


}
