package uk.ac.ebi.subs.api.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.validator.data.ValidationResult;

/**
 * EntityModel processor for {@link ValidationResult} entity used by Spring MVC controller.
 */
@Component
public class ValidationResultResourceProcessor implements RepresentationModelProcessor<EntityModel<ValidationResult>> {
    private static final Logger logger = LoggerFactory.getLogger(ValidationResultResourceProcessor.class);

    private RepositoryEntityLinks repositoryEntityLinks;

    public ValidationResultResourceProcessor(RepositoryEntityLinks repositoryEntityLinks) {
        this.repositoryEntityLinks = repositoryEntityLinks;
    }

    /**
     * Processes the given resource, add links, alter the domain data etc.
     *
     * @param resource
     * @return the processed resource
     */
    @Override
    public EntityModel<ValidationResult> process(EntityModel<ValidationResult> resource) {
        addSubmittableLink(resource);

        redactVerboseFields(resource);

        return resource;
    }

    private void addSubmittableLink(EntityModel<ValidationResult> resource) {
        String submittableType = resource.getContent().getEntityType();

        if (submittableType != null) {

            Class<?> submittableClass = null;
            try {
                submittableClass = Class.forName(submittableType);
            } catch (ClassNotFoundException e) {
                logger.error("Can't determine submittable class.", e);
            }

            if (submittableClass != null) {
                Link linkToItemResource = repositoryEntityLinks.linkToItemResource(
                    submittableClass,
                    resource.getContent().getEntityUuid()
                );

                Link submittableLink = linkToItemResource.withRel("submittable");
                resource.add(submittableLink);
            }
        }
    }

    private void redactVerboseFields(EntityModel<ValidationResult> resource) {
        resource.getContent().setEntityType(null);
        resource.getContent().setEntityUuid(null);
        resource.getContent().setSubmissionId(null);
        resource.getContent().setDataTypeId(null);
    }
}
