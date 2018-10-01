package uk.ac.ebi.subs.api.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.validator.data.ValidationResult;

@Component
public class ValidationResultResourceProcessor implements ResourceProcessor<Resource<ValidationResult>> {
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
    public Resource<ValidationResult> process(Resource<ValidationResult> resource) {

        addSubmittableLink(resource);

        redactVerboseFields(resource);

        return resource;
    }

    private void addSubmittableLink(Resource<ValidationResult> resource) {
        String submittableType = resource.getContent().getEntityType();

        if (submittableType != null) {

            Class<?> submittableClass = null;
            try {
                submittableClass = Class.forName(submittableType);
            } catch (ClassNotFoundException e) {
                logger.error("Can't determine submittable class.", e);
            }

            if (submittableClass != null) {
                Link linkToSingleResource = repositoryEntityLinks.linkToSingleResource(
                        submittableClass,
                        resource.getContent().getEntityUuid()
                );

                Link submittableLink = linkToSingleResource.withRel("submittable");
                resource.add(submittableLink);
            }
        }
    }

    private void redactVerboseFields(Resource<ValidationResult> resource) {
        resource.getContent().setEntityType(null);
        resource.getContent().setEntityUuid(null);
        resource.getContent().setSubmissionId(null);
        resource.getContent().setDataTypeId(null);
    }
}
