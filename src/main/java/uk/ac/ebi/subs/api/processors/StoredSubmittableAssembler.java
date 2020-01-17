package uk.ac.ebi.subs.api.processors;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.validator.data.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EntityModel assembler for {@link StoredSubmittable} for Spring MVC controller.
 */
@Component
@RequiredArgsConstructor
public class StoredSubmittableAssembler implements RepresentationModelAssembler<StoredSubmittable, EntityModel<StoredSubmittable>> {

    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;
    @NonNull
    private StoredSubmittableResourceProcessor<StoredSubmittable> storedSubmittableResourceProcessor;


    @Override
    public EntityModel<StoredSubmittable> toModel(StoredSubmittable entity) {

        EmbeddedWrappingResource resource = new EmbeddedWrappingResource(entity);

        List<Link> links = new ArrayList<>();

        links.add(repositoryEntityLinks.linkToItemResource(entity.getClass(), entity.getId()).withSelfRel());
        links.add(repositoryEntityLinks.linkToItemResource(entity.getClass(), entity.getId()));
        final Submission submission = resource.embeddedResources.submission;
        links.add(repositoryEntityLinks.linkToItemResource(submission.getClass(), submission.getId()));

        if (resource.embeddedResources.validationResult != null) {
            links.add(repositoryEntityLinks.linkToItemResource(ValidationResult.class, resource.embeddedResources.validationResult.getUuid()));
        }
        final ProcessingStatus processingStatus = resource.embeddedResources.processingStatus;
        if (processingStatus != null) {
            links.add(repositoryEntityLinks.linkToItemResource(processingStatus.getClass(), processingStatus.getId()));
        }
        final DataType dataType = resource.embeddedResources.dataType;
        if (dataType != null) {
            links.add(repositoryEntityLinks.linkToItemResource(dataType.getClass(), dataType.getId()));
        }
        final Checklist checklist = resource.embeddedResources.checklist;
        if (checklist != null) {
            links.add(repositoryEntityLinks.linkToItemResource(checklist.getClass(), checklist.getId()));
        }

        resource.add(
                links.stream().map(link -> link.expand()).collect(Collectors.toList())
        );

        return storedSubmittableResourceProcessor.process(resource);

    }

    @Data
    static class EmbeddedResources {
        private Submission submission;
        private ValidationResult validationResult;
        private ProcessingStatus processingStatus;
        private DataType dataType;
        private Checklist checklist;

        EmbeddedResources(StoredSubmittable storedSubmittable) {
            this.submission = storedSubmittable.getSubmission();
            this.validationResult = storedSubmittable.getValidationResult();
            this.processingStatus = storedSubmittable.getProcessingStatus();
            this.dataType = storedSubmittable.getDataType();
            this.checklist = storedSubmittable.getChecklist();
        }
    }

    static class EmbeddedWrappingResource extends EntityModel<StoredSubmittable> {
        EmbeddedWrappingResource(StoredSubmittable content) {
            super(content);
            wrapEmbeddedData(content);
        }

        @Getter
        @JsonProperty("_embedded")
        private EmbeddedResources embeddedResources;

        private void wrapEmbeddedData(StoredSubmittable storedSubmittable) {
            this.embeddedResources = new EmbeddedResources(storedSubmittable);
        }
    }
}


