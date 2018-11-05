package uk.ac.ebi.subs.api.processors;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.util.SchemaConverterFromMongo;

import java.util.HashMap;
import java.util.Map;

/**
 * Resource processor for {@link DataType} entity used by Spring MVC controller.
 */
@Component
@RequiredArgsConstructor
public class DataTypeResourceProcessor implements ResourceProcessor<Resource<DataType>> {

    @NonNull
    RepositoryEntityLinks repositoryEntityLinks;

    @Override
    public Resource<DataType> process(Resource<DataType> resource) {

        DataType dataType = resource.getContent();

        Link templatedChecklistsLink = repositoryEntityLinks.linkToSearchResource(Checklist.class, "by-data-type-id");

        Map<String, String> expansionParameters = new HashMap<>();
        expansionParameters.put("dataTypeId", dataType.getId());

        Link checklistLink = templatedChecklistsLink
                .expand(expansionParameters)
                .withRel("checklists");

        resource.add(checklistLink);


        // mongo can't store valid schema due to key constraints
        if (dataType.getValidationSchema() != null) {
            String originalSchema = dataType.getValidationSchema();
            JsonNode fixedSchema = SchemaConverterFromMongo.fixStoredJson(originalSchema);
            dataType.setValidationSchema(fixedSchema);
        }

        return resource;
    }
}
