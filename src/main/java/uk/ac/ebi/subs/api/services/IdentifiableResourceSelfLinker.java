package uk.ac.ebi.subs.api.services;

import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

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

    public Resource<T> addSelfLink(Resource<T> resource){
        resource.add(
                repositoryEntityLinks.linkForSingleResource(resource.getContent()).withSelfRel()
        );

        return resource;
    }
}
