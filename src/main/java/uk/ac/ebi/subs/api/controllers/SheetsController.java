package uk.ac.ebi.subs.api.controllers;

import lombok.Data;
import lombok.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.converters.SheetCsvMessageConverter;
import uk.ac.ebi.subs.api.processors.SpreadsheetResourceProcessor;
import uk.ac.ebi.subs.api.services.SheetService;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.SpreadsheetRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.security.PreAuthorizeSubmissionIdTeamName;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Dave on 21/10/2017.
 */
@Data
@RestController
@CrossOrigin
public class SheetsController {


    @NonNull
    private ApplicationEventPublisher publisher;

    @NonNull
    private SpreadsheetRepository spreadsheetRepository;

    @NonNull
    private SheetService sheetService;

    @NonNull
    private SubmissionRepository submissionRepository;

    @NonNull
    private ChecklistRepository checklistRepository;

    @NonNull
    private SheetCsvMessageConverter sheetCsvMessageConverter;

    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    @NonNull
    private SpreadsheetResourceProcessor spreadsheetResourceProcessor;

    @PreAuthorizeSubmissionIdTeamName
    @RequestMapping(path = "/submissions/{submissionId}/spreadsheet", method = RequestMethod.POST, consumes = {"text/csv", "text/csv;charset=UTF-8"})
    public ResponseEntity<Resource<Spreadsheet>> uploadCsv(
            @PathVariable @P("submissionId") String submissionId,
            @RequestParam @P("checklistId") String checklistId,
            InputStream inputStream) throws IOException {


        Submission submission = submissionRepository.findOne(submissionId);

        if (submission == null) {
            throw new ResourceNotFoundException();
        }

        Checklist checklist = checklistRepository.findOne(checklistId);

        if (checklist == null) {
            throw new ResourceNotFoundException();
        }

        //it should be possible to use Sheet directly, but the converter doesn't seem to be picked up by Spring
        Spreadsheet sheet = sheetCsvMessageConverter.readStream(inputStream);

        sheet.setSubmissionId(submission.getId());
        sheet.setTeam(submission.getTeam());
        sheet.setChecklistId(checklistId);
        sheet.setDataTypeId(checklist.getDataTypeId());

        sheetService.preProcessSheet(sheet);

        publisher.publishEvent(new BeforeCreateEvent(sheet));
        sheet = spreadsheetRepository.insert(sheet);
        publisher.publishEvent(new AfterCreateEvent(sheet));

        Resource<Spreadsheet> resource = new Resource<>(sheet);

        resource.add(
                repositoryEntityLinks.linkToSingleResource(Spreadsheet.class, sheet.getId()).withSelfRel(),
                repositoryEntityLinks.linkToSingleResource(Spreadsheet.class, sheet.getId())
        );

        resource = spreadsheetResourceProcessor.process(resource);


        ResponseEntity<Resource<Spreadsheet>> resourceSupportResponseEntity = new ResponseEntity<>(
                resource,
                HttpStatus.CREATED
        );

        return resourceSupportResponseEntity;
    }

}
