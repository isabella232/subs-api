package uk.ac.ebi.subs.api.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * EntityModel processor for {@link StoredSubmittable} entity used by Spring MVC controller.
 * @param <T> extends {@link StoredSubmittable}
 */
@Component
public class StoredSubmittableResourceProcessor<T extends StoredSubmittable> implements RepresentationModelProcessor<EntityModel<T>> {

    private static final Logger logger = LoggerFactory.getLogger(StoredSubmittableResourceProcessor.class);

    private RepositoryEntityLinks repositoryEntityLinks;
    private OperationControlService operationControlService;
    private LinkHelper linkHelper;

    public StoredSubmittableResourceProcessor(RepositoryEntityLinks repositoryEntityLinks, OperationControlService operationControlService, LinkHelper linkHelper) {
        this.repositoryEntityLinks = repositoryEntityLinks;
        this.operationControlService = operationControlService;
        this.linkHelper = linkHelper;
    }

    @Override
    public EntityModel<T> process(EntityModel<T> resource) {

        logger.debug("processing resource {}",resource);

        addHistory(resource);
        addCurrentVersion(resource);

        if (operationControlService.isUpdateable(resource.getContent())){
            resource.add(linkHelper.addSelfUpdateLink(new ArrayList<>(), resource.getContent()));
        }

        //redact content for internal use only
        resource.getContent().setReferences(null);
        resource.getContent().setValidationResult(null);
        resource.getContent().setProcessingStatus(null);
        resource.getContent().setDataType(null);
        resource.getContent().setChecklist(null);
        resource.getContent().setSubmission(null);

        return resource;
    }

    private void addHistory(EntityModel<? extends StoredSubmittable> resource) {
        StoredSubmittable item = resource.getContent();

        if (item.getTeam() != null && item.getTeam().getName() != null && item.getAlias() != null) {
            Map<String, String> expansionParams = new HashMap<>();

            expansionParams.put("teamName", item.getTeam().getName());
            expansionParams.put("alias", item.getAlias());

            Link contentsLink = repositoryEntityLinks.linkToSearchResource(item.getClass(), LinkRelation.of("history"));

            Assert.notNull(contentsLink);


            resource.add(
                    contentsLink.expand(expansionParams)
            );
        }
    }

    private void addCurrentVersion(EntityModel<? extends StoredSubmittable> resource) {
        StoredSubmittable item = resource.getContent();

        if (item.getTeam() != null && item.getTeam().getName() != null && item.getAlias() != null) {
            Map<String, String> expansionParams = new HashMap<>();

            expansionParams.put("teamName", item.getTeam().getName());
            expansionParams.put("alias", item.getAlias());

            Link contentsLink = repositoryEntityLinks.linkToSearchResource(item.getClass(), LinkRelation.of("current-version"));


            Assert.notNull(contentsLink);

            resource.add(
                    contentsLink.expand(expansionParams)
            );
        }
    }
}
