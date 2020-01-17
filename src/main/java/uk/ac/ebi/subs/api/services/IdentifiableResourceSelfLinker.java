package uk.ac.ebi.subs.api.services;

import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.Identifiable;

/**
 * This functions to add self links to Identifiable items wrapped as resources
 * This would normally be done by Spring Data Rest, but we need to add it ourselves when working with
 * controllers
 */
@Component
public class IdentifiableResourceSelfLinker<T extends Identifiable> {

    private RepositoryEntityLinks repositoryEntityLinks;

    public IdentifiableResourceSelfLinker(RepositoryEntityLinks repositoryEntityLinks) {
        this.repositoryEntityLinks = repositoryEntityLinks;
    }

    public EntityModel<T> addSelfLink(EntityModel<T> resource){
        resource.add(
                repositoryEntityLinks.linkForItemResource(resource.getContent().getClass(), resource.getContent().getId()).withSelfRel()
        );

        return resource;
    }
}
