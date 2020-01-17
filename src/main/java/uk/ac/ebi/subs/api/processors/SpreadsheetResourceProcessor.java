package uk.ac.ebi.subs.api.processors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;

/**
 * EntityModel processor for {@link Spreadsheet} entity used by Spring MVC controller.
 */
@Component
@RequiredArgsConstructor
public class SpreadsheetResourceProcessor implements RepresentationModelProcessor<EntityModel<Spreadsheet>> {
    private static final Logger logger = LoggerFactory.getLogger(SpreadsheetResourceProcessor.class);

    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    @Override
    public EntityModel<Spreadsheet> process(EntityModel<Spreadsheet> resource) {

        Spreadsheet spreadsheet = resource.getContent();

        //add links
        if (spreadsheet != null
                && spreadsheet.getSubmissionId() != null
                && spreadsheet.getChecklistId() != null
                && spreadsheet.getDataTypeId() != null) {
            resource.add(
                    repositoryEntityLinks.linkToItemResource(Submission.class, spreadsheet.getSubmissionId()),
                    repositoryEntityLinks.linkToItemResource(Checklist.class, spreadsheet.getChecklistId()),
                    repositoryEntityLinks.linkToItemResource(DataType.class, spreadsheet.getDataTypeId())
            );
        }


        //redact verbose material
        spreadsheet.setDataTypeId(null);
        spreadsheet.setChecklistId(null);
        spreadsheet.setSubmissionId(null);

        return resource;
    }
}
