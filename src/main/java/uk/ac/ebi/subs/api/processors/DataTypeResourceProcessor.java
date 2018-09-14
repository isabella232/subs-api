package uk.ac.ebi.subs.api.processors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataTypeResourceProcessor implements ResourceProcessor<Resource<DataType>> {

    @NonNull
    RepositoryEntityLinks repositoryEntityLinks;

    @Override
    public Resource<DataType> process(Resource<DataType> resource) {

        Link templatedChecklistsLink = repositoryEntityLinks.linkToSearchResource(Checklist.class, "by-data-type-id");

        Map<String,String> expansionParameters = new HashMap<>();
        expansionParameters.put("dataTypeId",resource.getContent().getId());

        Link checklistLink = templatedChecklistsLink
                .expand(expansionParameters)
                .withRel("checklists");

        resource.add(checklistLink);


        return resource;
    }
}
