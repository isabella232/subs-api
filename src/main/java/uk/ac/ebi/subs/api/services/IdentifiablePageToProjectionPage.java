package uk.ac.ebi.subs.api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.services.IdentifiableResourceSelfLinker;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class IdentifiablePageToProjectionPage<T extends Identifiable, P>  {

    private PagedResourcesAssembler<T> pagedResourcesAssembler;
    private SpelAwareProxyProjectionFactory projectionFactory;
    private IdentifiableResourceSelfLinker<T> identifiableResourceSelfLinker;

    public IdentifiablePageToProjectionPage(PagedResourcesAssembler<T> pagedResourcesAssembler, SpelAwareProxyProjectionFactory projectionFactory, IdentifiableResourceSelfLinker<T> identifiableResourceSelfLinker) {
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.projectionFactory = projectionFactory;
        this.identifiableResourceSelfLinker = identifiableResourceSelfLinker;
    }


    public PagedResources<Resource<P>> convert(Page<T> page, Pageable pageable, ResourceProcessor<Resource<T>> resourceProcessor, Class<P> projectionClass) {

        PagedResources<Resource<T>> pagedResources = pagedResourcesAssembler.toResource(page);

        PagedResources.PageMetadata pageMetadata = pagedResources.getMetadata();
        Collection<Link> pageLinks = pagedResources.getLinks();

        Collection<Resource<P>> submissionWithStatus = pagedResources.getContent().stream()
                .map(resource -> identifiableResourceSelfLinker.addSelfLink(resource))
                .map(resource -> resourceProcessor.process(resource))
                .map(resource ->
                        new Resource<>(
                                projectionFactory.createProjection(projectionClass, resource.getContent()),
                                resource.getLinks()
                        )
                )
                .collect(Collectors.toList());

        return new PagedResources<>(submissionWithStatus, pageMetadata, pageLinks);

    }
}
