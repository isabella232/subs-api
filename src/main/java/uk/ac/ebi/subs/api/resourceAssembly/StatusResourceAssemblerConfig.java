package uk.ac.ebi.subs.api.resourceAssembly;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import uk.ac.ebi.subs.data.status.Status;
import uk.ac.ebi.subs.api.StatusController;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Configuration
public class StatusResourceAssemblerConfig {

    @Bean
    public ResourceAssembler<Status, Resource<Status>> submissionStatusResourceAssembler() {
        return entity -> {
            Resource<Status> res = new Resource<Status>(entity);

            res.add(
                    linkTo(
                            methodOn(StatusController.class).submissionStatus(entity.getStatusName())
                    ).withSelfRel()
            );

            return res;
        };
    }

    @Bean
    public ResourceAssembler<Status, Resource<Status>> processingStatusResourceAssembler() {
        return entity -> {
            Resource<Status> res = new Resource<Status>(entity);

            res.add(
                    linkTo(
                            methodOn(StatusController.class).processingStatus(entity.getStatusName())
                    ).withSelfRel()
            );

            return res;
        };
    }

    @Bean
    public ResourceAssembler<Status, Resource<Status>> releaseStatusResourceAssembler() {
        return entity -> {
            Resource<Status> res = new Resource<Status>(entity);

            res.add(
                    linkTo(
                            methodOn(StatusController.class).releaseStatus(entity.getStatusName())
                    ).withSelfRel()
            );

            return res;
        };
    }


}
