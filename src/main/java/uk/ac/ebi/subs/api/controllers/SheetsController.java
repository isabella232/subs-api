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
import uk.ac.ebi.subs.api.services.SheetService;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.SheetRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.TemplateRepository;
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
    private SheetRepository sheetRepository;

    @NonNull
    private SheetService sheetService;

    @NonNull
    private SubmissionRepository submissionRepository;

    @NonNull
    private TemplateRepository templateRepository;

    @NonNull
    private SheetCsvMessageConverter sheetCsvMessageConverter;

    @NonNull
    private RepositoryEntityLinks repositoryEntityLinks;

    @PreAuthorizeSubmissionIdTeamName
    @RequestMapping(path = "/submissions/{submissionId}/spreadsheet", method = RequestMethod.POST, consumes = {"text/csv", "text/csv;charset=UTF-8"})
    public ResponseEntity<Resource<Sheet>> uploadCsv(
            @PathVariable @P("submissionId") String submissionId,
            @RequestParam @P("templateName") String templateName,
            InputStream inputStream) throws IOException {


        Submission submission = submissionRepository.findOne(submissionId);

        if (submission == null) {
            throw new ResourceNotFoundException();
        }


        Template template = templateRepository.findOneByName(templateName);

        if (template == null) {
            throw new ResourceNotFoundException();
        }

        //it should be possible to use Sheet directly, but the converter doesn't seem to be picked up by Spring
        Sheet sheet = sheetCsvMessageConverter.readStream(inputStream);

        sheet.setSubmission(submission);
        sheet.setTeam(submission.getTeam());
        sheet.setTemplate(template);

        sheetService.preProcessSheet(sheet);

        publisher.publishEvent(new BeforeCreateEvent(sheet));
        sheet = sheetRepository.insert(sheet);
        publisher.publishEvent(new AfterCreateEvent(sheet));

        Resource<Sheet> resource = new Resource<>(sheet);

        resource.add(
                repositoryEntityLinks.linkToSingleResource(Sheet.class, sheet.getId()).withSelfRel(),
                repositoryEntityLinks.linkToSingleResource(Sheet.class, sheet.getId()),
                repositoryEntityLinks.linkToSingleResource(Submission.class, submission.getId()),
                repositoryEntityLinks.linkToSingleResource(Template.class, template.getId())
        );


        ResponseEntity<Resource<Sheet>> resourceSupportResponseEntity = new ResponseEntity<>(
                resource,
                HttpStatus.CREATED
        );

        return resourceSupportResponseEntity;
    }

}
