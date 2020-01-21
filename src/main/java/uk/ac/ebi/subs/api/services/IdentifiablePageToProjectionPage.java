package uk.ac.ebi.subs.api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.Identifiable;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Converts a page of identifiable resources to a page of projections (Spring).
 * @param <T> identifiable resource as input
 * @param <P> projected resource as output
 */
@Component
public class IdentifiablePageToProjectionPage<T extends Identifiable, P>  {

    private PagedResourcesAssembler<T> pagedResourcesAssembler;
    private SpelAwareProxyProjectionFactory projectionFactory;
    private IdentifiableResourceSelfLinker<T> identifiableResourceSelfLinker;

    public IdentifiablePageToProjectionPage(PagedResourcesAssembler<T> pagedResourcesAssembler,
                                            SpelAwareProxyProjectionFactory projectionFactory,
                                            IdentifiableResourceSelfLinker<T> identifiableResourceSelfLinker) {
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.projectionFactory = projectionFactory;
        this.identifiableResourceSelfLinker = identifiableResourceSelfLinker;
    }


    public PagedModel<EntityModel<P>> convert(Page<T> page, Pageable pageable, RepresentationModelProcessor<EntityModel<T>> resourceProcessor, Class<P> projectionClass) {

        PagedModel<EntityModel<T>> pagedResources = pagedResourcesAssembler.toModel(page);

        PagedModel.PageMetadata pageMetadata = pagedResources.getMetadata();
        Links pageLinks = pagedResources.getLinks();

        Collection<EntityModel<P>> submissionWithStatus = pagedResources.getContent().stream()
                .map(resource -> identifiableResourceSelfLinker.addSelfLink(resource))
                .map(resource -> resourceProcessor.process(resource))
                .map(resource ->
                        new EntityModel<>(
                                projectionFactory.createProjection(projectionClass, resource.getContent()),
                                resource.getLinks()
                        )
                )
                .collect(Collectors.toList());

        return new PagedModel<>(submissionWithStatus, pageMetadata, pageLinks);

    }
}
