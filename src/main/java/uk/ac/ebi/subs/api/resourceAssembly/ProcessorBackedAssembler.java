package uk.ac.ebi.subs.api.resourceAssembly;

import org.springframework.hateoas.*;

public class ProcessorBackedAssembler<T extends Identifiable> implements ResourceAssembler<T, Resource<T>> {


    public ProcessorBackedAssembler(ResourceProcessor<Resource<T>> resourceProcessor, EntityLinks entityLinks) {
        this.resourceProcessor = resourceProcessor;
        this.entityLinks = entityLinks;
    }

    private ResourceProcessor<Resource<T>> resourceProcessor;
    private EntityLinks entityLinks;

    @Override
    public Resource<T> toResource(T entity) {
        Resource<T> resource = new Resource<T>(entity);

        if (resource.getContent() != null ) {
            Link link = entityLinks.linkToSingleResource(resource.getContent());
            resource.add(link);
            resource.add(link.withSelfRel());
        }

        return resourceProcessor.process(resource);
    }
}
