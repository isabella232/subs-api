package uk.ac.ebi.subs.api.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.data.submittable.Submittable;
import uk.ac.ebi.subs.api.resourceAssembly.SimpleResourceAssembler;
import uk.ac.ebi.subs.repository.repos.SubmittableRepository;

/**
 * Submittables will have standard controller endpoints available (e.g. find all items within a submission)
 * @param <T>
 */
public class SubmittableControllerSupport<T extends Submittable> {

    private SubmittableRepository<T> repository;
    private PagedResourcesAssembler<T> pagedResourcesAssembler;
    private SimpleResourceAssembler<T> simpleResourceAssembler;

    public SubmittableControllerSupport(
            SubmittableRepository<T> repository,
            PagedResourcesAssembler<T> pagedResourcesAssembler,
            SimpleResourceAssembler<T> simpleResourceAssembler) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.simpleResourceAssembler = simpleResourceAssembler;
    }

    public PagedResources<Resource<T>> submittablesInSubmission(String submissionId, Pageable pageable) {
        Page<T> page = repository.findBySubmissionId(submissionId, pageable);

        return pagedResourcesAssembler.toResource(
                page,
                simpleResourceAssembler
        );
    }

    public PagedResources<Resource<T>> submittableSubmissionHistory(String domainName, String alias, Pageable pageable){
        Page<T> page = repository.findByDomainNameAndAliasOrderByCreatedDateDesc(domainName, alias, pageable);

        return pagedResourcesAssembler.toResource(
                page,
                simpleResourceAssembler
        );
    }

    public Resource<T> submittableLatestVersion(String domainName, String alias){
        Pageable pageable = new PageRequest(0,1);
        Page<T> page = repository.findByDomainNameAndAliasOrderByCreatedDateDesc(domainName, alias, pageable);

        if (page.getTotalElements() == 0) {
            throw new ResourceNotFoundException();
        }

        Resource<T> resource = simpleResourceAssembler.toResource(page.getContent().get(0));

        return resource;
    }
}
