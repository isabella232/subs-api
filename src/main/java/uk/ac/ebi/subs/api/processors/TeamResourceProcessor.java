package uk.ac.ebi.subs.api.processors;

import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TeamResourceProcessor implements ResourceProcessor<Resource<Team>> {


    public TeamResourceProcessor(
            RepositoryEntityLinks repositoryEntityLinks,
            List<Class<? extends StoredSubmittable>> submittablesClassList
    ) {
        this.repositoryEntityLinks = repositoryEntityLinks;
        this.submittablesClassList = submittablesClassList;
    }

    private RepositoryEntityLinks repositoryEntityLinks;
    private List<Class<? extends StoredSubmittable>> submittablesClassList;


    @Override
    public Resource<Team> process(Resource<Team> resource) {


        addSubmissionsRel(resource);

        addContentsRels(resource);


        return resource;
    }

    private void addSubmissionsRel(Resource<Team> resource) {
        Map<String, String> expansionParams = new HashMap<>();
        expansionParams.put("teamName", resource.getContent().getName());

        addRelWithCollectionRelName(resource, expansionParams, Submission.class);
    }

    private void addContentsRels(Resource<Team> resource) {
        String teamName = resource.getContent().getName();
        Map<String, String> expansionParams = new HashMap<>();
        expansionParams.put("teamName", teamName);


        for (Class<? extends StoredSubmittable> submittableClass : submittablesClassList) {
            addRelWithCollectionRelName(resource, expansionParams, submittableClass);
        }

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
