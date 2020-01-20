package uk.ac.ebi.subs.api.processors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Links;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.controllers.SpreadsheetController;
import uk.ac.ebi.subs.api.controllers.SubmissionContentsController;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.Identifiable;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Submission;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Helper class to gather common utility methods to add {@link Link} to a Spring MVC resource.
 */
@Component
@Data
public class LinkHelper {

    private static final Logger logger = LoggerFactory.getLogger(LinkHelper.class);

    @NonNull
    private List<Class<? extends StoredSubmittable>> submittablesClassList;

    public static final String CREATE_REL_SUFFIX = ":create";
    public static final String SEARCH_REL_SUFFIX = ":search";
    public static final String UPDATE_REL_SUFFIX = ":update";
    public static final String DELETE_REL_SUFFIX = ":delete";

    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    public Link spreadsheetUploadLink(Submission submission){
        Link link = null;
        try {
            link =
                    linkTo(
                            methodOn(SpreadsheetController.class)
                                    .uploadCsv(
                                            submission.getId(),
                                            null,//template name is required, must select one and use as param
                                            null
                                    )
                    ).withRel("sheetUpload");
        } catch (IOException e) {
            logger.debug("Error has occurred when uploading a CSV file. The error caused by: ", e.getCause());
        }

        return link;
    }

    public Link submittableCreateLink(DataType dataType, Submission submission){
        String createRel = dataType.getId() + CREATE_REL_SUFFIX;

        Link submittablesCreateLink = linkTo(
                methodOn(SubmissionContentsController.class)
                        .createSubmissionContents(
                                submission.getId(),
                                dataType.getId(),
                                null
                        )
        )
                .withRel(createRel)
                .expand();

        return submittablesCreateLink;
    }

    public void addSubmittableCreateLink(Collection<Link> links, DataType type, Submission submission) {
        links.add(submittableCreateLink(type,submission));
    }

    public Collection<Link> addSubmittablesInTeamLinks(Collection<Link> links, String teamName) {
        Map<String, String> params = new HashMap<>();
        params.put("teamName", teamName);

        return this.addSubmittablesLinksWithNamedSearchRel(links, "by-team", params);
    }

    private Collection<Link> addSubmittablesLinksWithNamedSearchRel(Collection<Link> links, String relName, Map<String, String> expansionParams) {

        for (Class type : submittablesClassList) {
            if (!type.equals(Project.class)) {
                Link searchLink = repositoryEntityLinks.linkToSearchResource(type, LinkRelation.of(relName));
                Link collectionLink = repositoryEntityLinks.linkToCollectionResource(type).expand();

                Link submittablesInSubmission = searchLink.expand(expansionParams).withRel(collectionLink.getRel());
                links.add(submittablesInSubmission);
            }
        }

        return links;
    }

    public Collection<Link> addSelfUpdateLink(Collection<Link> links, Identifiable<?> identifiable) {
        Link singleResourceLink = repositoryEntityLinks.linkToItemResource(
                identifiable.getClass(), identifiable.getId()).expand();

        Assert.notNull(singleResourceLink);

        Link updateLink = singleResourceLink.withRel("self" + UPDATE_REL_SUFFIX);

        links.add(updateLink);

        Link deleteLink = singleResourceLink.withRel("self" + DELETE_REL_SUFFIX);

        links.add(deleteLink);

        return links;
    }

    public void addSearchLink(Collection<Link> links, Class type) {
        Link collectionLink = repositoryEntityLinks.linkToCollectionResource(type).expand();

        LinkRelation relBase = collectionLink.getRel();

        Links searchLinks = repositoryEntityLinks.linksToSearchResources(type);

        if (searchLinks == null || searchLinks.isEmpty()) {
            logger.debug("No search links found for class {}", type);
        } else {
            logger.debug("Search links found for class {}: {} ", type, searchLinks);

            String href = collectionLink.getHref() + "/search";
            String rel = relBase.value() + SEARCH_REL_SUFFIX;
            Link searchesLink = new Link(href, rel);

            links.add(searchesLink);
        }
    }
}
