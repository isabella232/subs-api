package uk.ac.ebi.subs.api.processors;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.controllers.SpreadsheetTemplateController;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.util.SchemaConverterFromMongo;

import java.io.IOException;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * EntityModel processor for {@link Checklist} entity used by Spring MVC controller.
 */
@Component
public class ChecklistResourceProcessor implements RepresentationModelProcessor<EntityModel<Checklist>> {

    @Override
    public EntityModel<Checklist> process(EntityModel<Checklist> resource) {
        Checklist checklist = resource.getContent();
        String checklistId = checklist.getId();

        Link baseDownloadLink = linkTo(methodOn(SpreadsheetTemplateController.class).templateAsSheet(checklistId))
                .withRel("placeholder");
        String baseDownloadHref = baseDownloadLink.getHref();

        resource.add(new Link(baseDownloadHref + ".csv", "spreadsheet-csv-download"));

        // mongo can't store valid schema due to key constraints
        if (checklist.getValidationSchema() != null) {
            String originalSchema = checklist.getValidationSchema();
            JsonNode fixedSchema = SchemaConverterFromMongo.fixStoredJson(originalSchema);
            checklist.setValidationSchema(fixedSchema);
        }

        return resource;
    }
}
