package uk.ac.ebi.subs.api.resourceAssembly;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.controllers.DomainController;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;


@Component
public class SubmissionResourceProcessor implements ResourceProcessor<Resource<Submission>> {

    @Autowired
    public SubmissionResourceProcessor(
            SubmissionStatusRepository submissionStatusRepository,
            RepositoryEntityLinks repositoryEntityLinks,
            List<Class<? extends StoredSubmittable>> submittablesClassList
    ) {
        this.submissionStatusRepository = submissionStatusRepository;
        this.repositoryEntityLinks = repositoryEntityLinks;
        this.submittablesClassList = submittablesClassList;

        this.defaultPageRequest = new PageRequest(0, 1);

    }


    private SubmissionStatusRepository submissionStatusRepository;
    private RepositoryEntityLinks repositoryEntityLinks;
    private List<Class<? extends StoredSubmittable>> submittablesClassList;

    private Pageable defaultPageRequest;


    @Override
    public Resource<Submission> process(Resource<Submission> resource) {

        addDomainRel(resource);
        addContentsRels(resource);

        return resource;
    }

    private void addContentsRels(Resource<Submission> resource) {
        Map<String, String> expansionParams = new HashMap<>();
        expansionParams.put("submissionId", resource.getContent().getId());

        for (Class<? extends StoredSubmittable> submittableClass : submittablesClassList) {
            Link contentsLink = repositoryEntityLinks.linkToSearchResource(submittableClass, "by-submission", defaultPageRequest);
            Link collectionLink = repositoryEntityLinks.linkToCollectionResource(submittableClass);

            Assert.notNull(contentsLink);
            Assert.notNull(collectionLink);


            resource.add(
                    contentsLink.expand(expansionParams).withRel(collectionLink.getRel())
            );

        }

    }

    private void addDomainRel(Resource<Submission> resource) {
        if (resource.getContent().getDomain() != null && resource.getContent().getDomain().getName() != null) {
            resource.add(
                    linkTo(
                            methodOn(DomainController.class)
                                    .getDomain(resource.getContent().getDomain().getName())
                    ).withRel("domain")
            );
        }
    }


}
