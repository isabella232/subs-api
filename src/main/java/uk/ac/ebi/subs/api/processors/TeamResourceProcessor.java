package uk.ac.ebi.subs.api.processors;

import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.controllers.TeamItemsController;
import uk.ac.ebi.subs.api.controllers.TeamSubmissionController;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Submission;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * EntityModel processor for {@link Team} entity used by Spring MVC controller.
 */
@Component
public class TeamResourceProcessor implements RepresentationModelProcessor<EntityModel<Team>> {

    public TeamResourceProcessor(RepositoryEntityLinks repositoryEntityLinks, LinkHelper linkHelper) {
        this.repositoryEntityLinks = repositoryEntityLinks;
        this.linkHelper = linkHelper;
    }

    private RepositoryEntityLinks repositoryEntityLinks;
    private LinkHelper linkHelper;

    @Override
    public EntityModel<Team> process(EntityModel<Team> resource) {
        addSubmissionsRel(resource);

        addItemsRel(resource);

        return resource;
    }

    private void addItemsRel(EntityModel<Team> resource) {
        if (resource.getLink("items").isEmpty()) {
            resource.add(
                linkTo(methodOn(TeamItemsController.class).teamItems(resource.getContent().getName())
                ).withRel("items")
            );
        }
    }

    private void addSubmissionsRel(EntityModel<Team> resource) {
        addGetSubmissionsRel(resource);
        addCreateSubmissionRel(resource);
    }

    private void addCreateSubmissionRel(EntityModel<Team> resource) {
        String submissionsRel = "submissions" + LinkHelper.CREATE_REL_SUFFIX;

        if (resource.getLink(submissionsRel).isPresent()) {
            return;
        }

        Map<String, String> expansionParams = new HashMap<>();
        expansionParams.put("repository", "submissions");

        Link submissionCreateLink = linkTo(
            methodOn(TeamSubmissionController.class)
                .createTeamSubmission(
                    resource.getContent().getName(),
                    null,
                    null,
                    null
                )
        ).withRel(submissionsRel)
            .expand(expansionParams);

        resource.add(submissionCreateLink);
    }

    private void addGetSubmissionsRel(EntityModel<Team> resource) {
        Map<String, String> expansionParams = new HashMap<>();
        expansionParams.put("teamName", resource.getContent().getName());

        addRelWithCollectionRelName(resource, expansionParams, Submission.class);
    }

    private void addRelWithCollectionRelName(EntityModel<Team> resource, Map<String, String> expansionParams, Class<?> classWithByTeamRel) {
        Link contentsLink = repositoryEntityLinks.linkToSearchResource(classWithByTeamRel, LinkRelation.of("by-team"));
        Link collectionLink = repositoryEntityLinks.linkToCollectionResource(classWithByTeamRel);

        Assert.notNull(contentsLink);
        Assert.notNull(collectionLink);

        if (resource.getLink(collectionLink.getRel()).isEmpty()) {
            resource.add(
                contentsLink.expand(expansionParams).withRel(collectionLink.getRel())
            );
        }
    }
}
