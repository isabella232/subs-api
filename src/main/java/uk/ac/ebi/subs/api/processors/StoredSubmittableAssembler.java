package uk.ac.ebi.subs.api.processors;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.validator.data.ValidationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StoredSubmittableAssembler implements ResourceAssembler<StoredSubmittable,Resource<StoredSubmittable>> {

    @NonNull private RepositoryEntityLinks repositoryEntityLinks;

    @Override
    public Resource<StoredSubmittable> toResource(StoredSubmittable entity) {

        EmbeddedWrappingResource resource =  new EmbeddedWrappingResource(entity);

        List<Link> links = new ArrayList();

        links.add(repositoryEntityLinks.linkToSingleResource(entity).withSelfRel());
        links.add(repositoryEntityLinks.linkToSingleResource(entity));
        links.add(repositoryEntityLinks.linkToSingleResource(resource.embeddedResources.submission));
        links.add(repositoryEntityLinks.linkToSingleResource(ValidationResult.class,resource.embeddedResources.validationResult.getUuid()));
        links.add(repositoryEntityLinks.linkToSingleResource(resource.embeddedResources.processingStatus));
        links.add(repositoryEntityLinks.linkToSingleResource(resource.embeddedResources.dataType));

        if (resource.embeddedResources.checklist != null) {
            links.add(repositoryEntityLinks.linkToSingleResource(resource.embeddedResources.checklist));
        }

        resource.add(
                links.stream().map(link -> link.expand()).collect(Collectors.toList())
        );

        return resource;

    }

    @Data
    static class EmbeddedResources {
        private Submission submission;
        private ValidationResult validationResult;
        private ProcessingStatus processingStatus;
        private DataType dataType;
        private Checklist checklist;

        EmbeddedResources(StoredSubmittable storedSubmittable){
            this.submission = storedSubmittable.getSubmission();
            this.validationResult = storedSubmittable.getValidationResult();
            this.processingStatus = storedSubmittable.getProcessingStatus();
            this.dataType = storedSubmittable.getDataType();
            this.checklist = storedSubmittable.getChecklist();


        }
    }

    @Data
    static class EmbeddedWrappingResource extends Resource<StoredSubmittable> {
        public EmbeddedWrappingResource(StoredSubmittable content, Link... links) {
            super(content, links);
            wrapEmbeddedData(content);
        }

        public EmbeddedWrappingResource(StoredSubmittable content, Iterable<Link> links) {
            super(content, links);
            wrapEmbeddedData(content);
        }

        @JsonProperty("_embedded")
        private EmbeddedResources embeddedResources;

        private void wrapEmbeddedData(StoredSubmittable storedSubmittable){
           this.embeddedResources = new EmbeddedResources(storedSubmittable);
        }
    }
}


