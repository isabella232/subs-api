package uk.ac.ebi.subs.api.resourceAssembly;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.*;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;

@Component
public class SimpleResourceAssembler<T extends Identifiable> implements ResourceAssembler<T, Resource<T>> {


    public SimpleResourceAssembler(@Autowired EntityLinks entityLinks) {

        this.entityLinks = entityLinks;
    }

    private ResourceProcessor<Resource<T>> resourceProcessor;
    private EntityLinks entityLinks;

    @Override
    public Resource<T> toResource(T entity) {
        Resource<T> resource = new Resource<T>(entity);

        if (resource.getContent() != null) {
            Link link = entityLinks.linkToSingleResource(resource.getContent());
            resource.add(link);
            resource.add(link.withSelfRel());
        }

        if (
                resource.getContent() != null &&
                        resource.getContent() instanceof StoredSubmittable &&
                        ((StoredSubmittable) resource.getContent()).getSubmission() != null
                ) {
            StoredSubmittable storedSubmittable = (StoredSubmittable) resource.getContent();
            Link link = entityLinks.linkToSingleResource(storedSubmittable.getSubmission());
            resource.add(link);
        }

        return resource;
    }
}
