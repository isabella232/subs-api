package uk.ac.ebi.subs.api.processors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;

/**
 * Resource processor for {@link Spreadsheet} entity used by Spring MVC controller.
 */
@Component
@RequiredArgsConstructor
public class SpreadsheetResourceProcessor implements ResourceProcessor<Resource<Spreadsheet>> {
    private static final Logger logger = LoggerFactory.getLogger(SpreadsheetResourceProcessor.class);

    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    @Override
    public Resource<Spreadsheet> process(Resource<Spreadsheet> resource) {

        Spreadsheet spreadsheet = resource.getContent();

        //add links
        resource.add(
            repositoryEntityLinks.linkToSingleResource(Submission.class, spreadsheet.getSubmissionId()),
            repositoryEntityLinks.linkToSingleResource(Checklist.class, spreadsheet.getChecklistId()),
            repositoryEntityLinks.linkToSingleResource(DataType.class, spreadsheet.getDataTypeId())
        );

        //redact verbose material
        spreadsheet.setDataTypeId(null);
        spreadsheet.setChecklistId(null);
        spreadsheet.setSubmissionId(null);

        return resource;
    }
}
