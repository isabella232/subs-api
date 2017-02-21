package uk.ac.ebi.subs.api.resourceAssembly;

import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.data.component.Domain;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DomainResourceProcessor implements ResourceProcessor<Resource<Domain>> {


    public DomainResourceProcessor(
            RepositoryEntityLinks repositoryEntityLinks,
            List<Class<? extends StoredSubmittable>> submittablesClassList
    ) {
        this.repositoryEntityLinks = repositoryEntityLinks;
        this.submittablesClassList = submittablesClassList;
    }

    private RepositoryEntityLinks repositoryEntityLinks;
    private List<Class<? extends StoredSubmittable>> submittablesClassList;


    @Override
    public Resource<Domain> process(Resource<Domain> resource) {


        addSubmissionsRel(resource);

        addContentsRels(resource);


        return resource;
    }

    private void addSubmissionsRel(Resource<Domain> resource) {
        String domainName = resource.getContent().getName();
        Map<String, String> expansionParams = new HashMap<>();
        expansionParams.put("domainName", domainName);

        addRelWithCollectionRelName(resource, expansionParams, Submission.class);
    }

    private void addContentsRels(Resource<Domain> resource) {
        String domainName = resource.getContent().getName();
        Map<String, String> expansionParams = new HashMap<>();
        expansionParams.put("domainName", domainName);


        for (Class<? extends StoredSubmittable> submittableClass : submittablesClassList) {
            addRelWithCollectionRelName(resource, expansionParams, submittableClass);
        }

    }

    private void addRelWithCollectionRelName(Resource<Domain> resource, Map<String, String> expansionParams, Class<?> classWithByDomainRel) {
        Link contentsLink = repositoryEntityLinks.linkToSearchResource(classWithByDomainRel, "by-domain");
        Link collectionLink = repositoryEntityLinks.linkToCollectionResource(classWithByDomainRel);

        Assert.notNull(contentsLink);
        Assert.notNull(collectionLink);


        resource.add(
                contentsLink.expand(expansionParams).withRel(collectionLink.getRel())
        );
    }
}
