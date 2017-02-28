package uk.ac.ebi.subs.api.resourceAssembly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.controllers.DomainController;
import uk.ac.ebi.subs.api.controllers.StatusDescriptionController;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class RootEndpointLinkProcessor implements ResourceProcessor<RepositoryLinksResource> {

    private static final Logger logger = LoggerFactory.getLogger(RootEndpointLinkProcessor.class);

    public RootEndpointLinkProcessor(
            List<Class<? extends StoredSubmittable>> submittablesClassList,
            RepositoryEntityLinks repositoryEntityLinks
    ) {
        this.submittablesClassList = submittablesClassList;
        this.repositoryEntityLinks = repositoryEntityLinks;

    }

    private List<Class<? extends StoredSubmittable>> submittablesClassList;
    private RepositoryEntityLinks repositoryEntityLinks;


    private List<Link> createLinks() {
        List<Link> links = new ArrayList<>();

        addStatusDescriptions(links);
        addSubmittables(links);
        addSubmissions(links);
        addDomain(links);

        return links;
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {

       clearAllLinksButProfile(resource);

       resource.add(createLinks());

        return resource;
    }

    private void clearAllLinksButProfile(RepositoryLinksResource resource) {
        Link profileLink = resource.getLink("profile");
        resource.removeLinks();
        if (profileLink != null) {
            resource.add(profileLink);
        }
    }

    private void addStatuses(List<Link> links) {
        List<Class> statusClasses = Arrays.asList(ProcessingStatus.class, SubmissionStatus.class);

        for (Class clazz : statusClasses) {
            addStandardLinks(links, clazz, false);
        }
    }

    private void addSubmissions(List<Link> links) {
        addStandardLinks(links, Submission.class, true);
    }

    private void addSubmittables(List<Link> links) {
        for (Class clazz : submittablesClassList) {
            addStandardLinks(links, clazz, true);
        }
    }

    private void addStandardLinks(List<Link> links, Class clazz, boolean create) {
        Link collectionLink = repositoryEntityLinks.linkToCollectionResource(clazz).expand();

        String relBase = collectionLink.getRel();

        if (create) {
            links.add(collectionLink.withRel(relBase + ":create"));
        }

        Links searchLinks = repositoryEntityLinks.linksToSearchResources(clazz);

        if (searchLinks == null || searchLinks.isEmpty()) {
            logger.error("No search links found for class {}",clazz);
        }
        else {
            logger.debug("Search links found for clazz {}: {} ",clazz,searchLinks);

            String href = collectionLink.getHref() + "/search";
            String rel = relBase + ":search";
            Link searchesLink = new Link(href,rel);

            links.add(searchesLink);

        }
    }

    private void addDomain(List<Link> links) {
        links.add(
                linkTo(methodOn(DomainController.class).getDomain(null)).withRel("domain")
        );
    }

    private void addStatusDescriptions(List<Link> links) {
        links.add(
                linkTo(
                        methodOn(StatusDescriptionController.class)
                                .allProcessingStatus(null))
                        .withRel("processingStatusDescriptions")
        );
        links.add(
                linkTo(
                        methodOn(StatusDescriptionController.class)
                                .allReleaseStatus(null))
                        .withRel("releaseStatusDescriptions")
        );
        links.add(
                linkTo(
                        methodOn(StatusDescriptionController.class)
                                .allSubmissionStatus(null))
                        .withRel("submissionStatusDescriptions")
        );
    }
}

