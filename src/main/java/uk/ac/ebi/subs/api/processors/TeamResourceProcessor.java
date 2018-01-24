package uk.ac.ebi.subs.api.processors;

import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.controllers.TeamItemsController;
import uk.ac.ebi.subs.api.controllers.TeamSubmissionController;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Submission;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class TeamResourceProcessor implements ResourceProcessor<Resource<Team>> {

    public TeamResourceProcessor(RepositoryEntityLinks repositoryEntityLinks, LinkHelper linkHelper) {
        this.repositoryEntityLinks = repositoryEntityLinks;
        this.linkHelper = linkHelper;
    }

    private RepositoryEntityLinks repositoryEntityLinks;
    private LinkHelper linkHelper;


    @Override
    public Resource<Team> process(Resource<Team> resource) {


        addSubmissionsRel(resource);

        addItemsRel(resource);


        return resource;
    }

    private void addItemsRel(Resource<Team> resource) {
        resource.getLinks().add(
                linkTo(methodOn(TeamItemsController.class).teamItems(resource.getContent().getName())
                ).withRel("items")
        );
    }

    private void addSubmissionsRel(Resource<Team> resource) {
        addGetSubmissionsRel(resource);


        Map<String,String> expansionParams= new HashMap<>();
        expansionParams.put("repository","submissions");

        Link submissionCreateLink = linkTo(
                methodOn(TeamSubmissionController.class)
                        .createTeamSubmission(
                                resource.getContent().getName(),
                                null,
                                null
                        )
        ).withRel("submissions"+LinkHelper.CREATE_REL_SUFFIX)
                .expand(expansionParams);

        resource.add(submissionCreateLink);
    }

    private void addGetSubmissionsRel(Resource<Team> resource) {
        Map<String, String> expansionParams = new HashMap<>();
        expansionParams.put("teamName", resource.getContent().getName());

        addRelWithCollectionRelName(resource, expansionParams, Submission.class);
    }


    private void addRelWithCollectionRelName(Resource<Team> resource, Map<String, String> expansionParams, Class<?> classWithByTeamRel) {
        Link contentsLink = repositoryEntityLinks.linkToSearchResource(classWithByTeamRel, "by-team");
        Link collectionLink = repositoryEntityLinks.linkToCollectionResource(classWithByTeamRel);

        Assert.notNull(contentsLink);
        Assert.notNull(collectionLink);


        resource.add(
                contentsLink.expand(expansionParams).withRel(collectionLink.getRel())
        );
    }
}
