package uk.ac.ebi.subs.api.processors;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.controllers.TeamItemsController;
import uk.ac.ebi.subs.api.controllers.TemplateController;
import uk.ac.ebi.subs.repository.model.templates.Template;

import java.io.IOException;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class TemplateResourceProcessor implements ResourceProcessor<Resource<Template>> {

    @Override
    public Resource<Template> process(Resource<Template> resource) {
        Template template = resource.getContent();
        String templateId = template.getId();

        String baseDownloadHref = "";

        try {
            Link baseDownloadLink = linkTo(methodOn(TemplateController.class).templateAsSheet(templateId)).withRel("placeholder");
            baseDownloadHref = baseDownloadLink.getHref();
        } catch (IOException e) {
            // method is not actually invoked, so this exception can't happen
        }
        resource.add(
                new Link(baseDownloadHref+".csv","spreadsheet-csv-download")

        );

        return resource;
    }
}
