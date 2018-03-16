package uk.ac.ebi.subs.api.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;

import java.util.HashMap;
import java.util.Map;

@Component
public class StoredSubmittableResourceProcessor<T extends StoredSubmittable> implements ResourceProcessor<Resource<T>> {

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
    public Resource<T> process(Resource<T> resource) {

        logger.debug("processing resource {}",resource);

        addHistory(resource);
        addCurrentVersion(resource);

        if (operationControlService.isUpdateable(resource.getContent())){
            linkHelper.addSelfUpdateLink(resource.getLinks(),resource.getContent());
        }


        return resource;
    }

    private void addHistory(Resource<? extends StoredSubmittable> resource) {
        StoredSubmittable item = resource.getContent();

        if (item.getTeam() != null && item.getTeam().getName() != null && item.getAlias() != null) {
            Map<String, String> expansionParams = new HashMap<>();

            expansionParams.put("teamName", item.getTeam().getName());
            expansionParams.put("alias", item.getAlias());

            Link contentsLink = repositoryEntityLinks.linkToSearchResource(item.getClass(), "history");

            Assert.notNull(contentsLink);


            resource.add(
                    contentsLink.expand(expansionParams)
            );

        }
    }

    private void addCurrentVersion(Resource<? extends StoredSubmittable> resource) {
        StoredSubmittable item = resource.getContent();

        if (item.getTeam() != null && item.getTeam().getName() != null && item.getAlias() != null) {
            Map<String, String> expansionParams = new HashMap<>();

            expansionParams.put("teamName", item.getTeam().getName());
            expansionParams.put("alias", item.getAlias());

            Link contentsLink = repositoryEntityLinks.linkToSearchResource(item.getClass(), "current-version");


            Assert.notNull(contentsLink);

            resource.add(
                    contentsLink.expand(expansionParams)
            );

        }
    }
}
