package uk.ac.ebi.subs.api.processors;

import lombok.Data;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.controllers.SubmissionContentsController;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
@Component
@Data
public class LinkHelper {

    private static final Logger logger = LoggerFactory.getLogger(LinkHelper.class);

    @NonNull private List<Class<? extends StoredSubmittable>> submittablesClassList;

    static final String CREATE_REL_SUFFIX = ":create";
    static final String SEARCH_REL_SUFFIX = ":search";
    static final String UPDATE_REL_SUFFIX = ":update";
    static final String DELETE_REL_SUFFIX = ":delete";

    @NonNull private RepositoryEntityLinks repositoryEntityLinks;
    @NonNull private ProjectRepository projectRepository;

    public List<Class<? extends StoredSubmittable>> getSubmittablesClassList() {
        return submittablesClassList;
    }

    public void addSubmittablesSearchLinks(Collection<Link> links){
        for (Class type : submittablesClassList){
            this.addSearchLink(links,type);
        }
    }

    public void addSubmittablesCreateLinks(Submission submission, Collection<Link> links){
        Assert.notNull(submission.getId());

        for (Class type : submittablesClassList){
            if (type.equals(Project.class)){
                this.addProjectCreateLink(links,submission);
            }
            else {
                this.addSubmittableCreateLink(links, type, submission);
            }
        }
    }

    public void addProjectCreateLink(Collection<Link> links, Submission submission){
        if (projectRepository.findOneBySubmissionId(submission.getId()) == null){
            this.addSubmittableCreateLink(links,Project.class,submission);
        }
    }

    public void addSubmittableCreateLink(Collection<Link> links, Class type, Submission submission) {

        Link collectionLink = repositoryEntityLinks.linkToCollectionResource(type).expand();
        String relBase = collectionLink.getRel();

        String createRel = relBase + CREATE_REL_SUFFIX;

        Map<String,String> expansionParams= new HashMap<>();
        expansionParams.put("repository",relBase);


        Link submittablesCreateLink = linkTo(
                methodOn(SubmissionContentsController.class)
                        .createSubmissionContents(
                                submission.getId(),
                                null,
                                null,
                                null,
                                null
                        )
        ).withRel(createRel)
                .expand(expansionParams);

        links.add(submittablesCreateLink);
    }

    public void addSubmittablesInSubmissionLinks(Collection<Link> links, String submissionId){
        Map<String,String> params = new HashMap<>();
        params.put("submissionId",submissionId);

        this.addSubmittablesLinksWithNamedSearchRel(links,"by-submission",params);
    }

    public void addSubmittablesInTeamLinks(Collection<Link> links, String teamName){
        Map<String,String> params = new HashMap<>();
        params.put("teamName",teamName);

        this.addSubmittablesLinksWithNamedSearchRel(links,"by-team",params);
    }

    private void addSubmittablesLinksWithNamedSearchRel(Collection<Link> links, String relName, Map<String,String> expansionParams){

        for (Class type : submittablesClassList){
            if (!type.equals(Project.class)){
                Link searchLink = repositoryEntityLinks.linkToSearchResource(type, relName);
                Link collectionLink = repositoryEntityLinks.linkToCollectionResource(type).expand();

                Link submittablesInSubmission = searchLink.expand(expansionParams).withRel(collectionLink.getRel());
                links.add(submittablesInSubmission);
            }
        }
    }

    public void addSelfUpdateLink(Collection<Link> links, Identifiable<?> identifiable){
        Link singleResourceLink = repositoryEntityLinks.linkToSingleResource(identifiable).expand();

        Assert.notNull(singleResourceLink);

        Link updateLink = singleResourceLink.withRel( "self" + UPDATE_REL_SUFFIX );

        links.add(updateLink);

        Link deleteLink = singleResourceLink.withRel( "self" + DELETE_REL_SUFFIX );

        links.add(deleteLink);
    }



    public void addCreateLink(Collection<Link> links, Class type) {

        Link collectionLink = repositoryEntityLinks.linkToCollectionResource(type).expand();

        String relBase = collectionLink.getRel();

        links.add(collectionLink.withRel(relBase + CREATE_REL_SUFFIX));
    }

    public void addSearchLink(Collection<Link> links, Class type) {
                Link collectionLink = repositoryEntityLinks.linkToCollectionResource(type).expand();

        String relBase = collectionLink.getRel();


        Links searchLinks = repositoryEntityLinks.linksToSearchResources(type);

        if (searchLinks == null || searchLinks.isEmpty()) {
            logger.debug("No search links found for class {}", type);
        } else {
            logger.debug("Search links found for class {}: {} ", type, searchLinks);

            String href = collectionLink.getHref() + "/search";
            String rel = relBase + SEARCH_REL_SUFFIX;
            Link searchesLink = new Link(href, rel);

            links.add(searchesLink);

        }
    }



}
