package uk.ac.ebi.subs.api.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.controllers.StatusDescriptionController;
import uk.ac.ebi.subs.api.controllers.StudyDataTypeController;
import uk.ac.ebi.subs.api.controllers.TeamController;
import uk.ac.ebi.subs.api.controllers.UserProjectsController;
import uk.ac.ebi.subs.api.controllers.UserSubmissionsController;

import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class RootEndpointLinkProcessor implements ResourceProcessor<RepositoryLinksResource> {

    private static final Logger logger = LoggerFactory.getLogger(RootEndpointLinkProcessor.class);

    public RootEndpointLinkProcessor(RepositoryEntityLinks repositoryEntityLinks, LinkHelper linkHelper) {
        this.repositoryEntityLinks = repositoryEntityLinks;
        this.linkHelper = linkHelper;
    }

    private RepositoryEntityLinks repositoryEntityLinks;
    private LinkHelper linkHelper;

    private void addLinks(List<Link> links) {
        addStatusDescriptions(links);
        addTeams(links);
        addUserProjects(links);
        addUserSubmissions(links);
        addStudyDataType(links);
    }

    private void addStudyDataType(List<Link> links) {
        Link studyDataTypeLink =
                linkTo(methodOn(StudyDataTypeController.class).getStudyDataTypes())
                    .withRel("studyDataTypes");

        links.add(studyDataTypeLink);
    }

    private void addUserProjects(List<Link> links) {
        Link userProjectsLink =
                linkTo(methodOn(UserProjectsController.class).getUserProjects(null)
                ).withRel("userProjects");

        links.add(userProjectsLink);
    }

    private void addUserSubmissions(List<Link> links) {
        Link userSubmissionsLink =
                linkTo(methodOn(UserSubmissionsController.class).getUserSubmissions(null)
                ).withRel("userSubmissions");

        links.add(userSubmissionsLink);

        Link userSubmissionStatusSummaryLink =
                linkTo(methodOn(UserSubmissionsController.class).getUserSubmissionStatusSummary()
                ).withRel("userSubmissionStatusSummary");

        links.add(userSubmissionStatusSummaryLink);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        logger.debug("processing resource: {}", resource.getLinks());
        clearAllLinks(resource);

        addLinks(resource.getLinks());

        return resource;
    }

    private void clearAllLinks(RepositoryLinksResource resource) {
        logger.debug("clearing links: {}", resource.getLinks());
        resource.removeLinks();
    }

    private void addTeams(List<Link> links) {
        Link teamsLink =
                linkTo(methodOn(TeamController.class).getTeams(null)
                ).withRel("userTeams");

        links.add(teamsLink);

        ControllerLinkBuilder linkBuilder = linkTo(methodOn(TeamController.class).getTeam(null));
        links.add(linkBuilder.withRel("team"));
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

