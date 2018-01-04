package uk.ac.ebi.subs.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NonNull;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.subs.api.converters.SheetCsvMessageConverter;
import uk.ac.ebi.subs.api.services.PersistentEntityCreationHelper;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.SheetRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.TemplateRepository;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Dave on 21/10/2017.
 */
@Data
@RepositoryRestController
public class SheetsController {

    @NonNull
    private PersistentEntityCreationHelper persistentEntityCreationHelper;

    @NonNull
    private SheetRepository sheetRepository;

    @NonNull
    private SubmissionRepository submissionRepository;

//  @NonNull private SheetHelper sheetHelper;

    @NonNull
    private TemplateRepository templateRepository;


    @NonNull
    private ObjectMapper objectMapper;

    @NonNull
    private SheetCsvMessageConverter sheetCsvMessageConverter;

    @RequestMapping(path = "/submissions/{submissionId}/contents/{targetType}/{repository}", method = RequestMethod.POST, consumes = {"text/csv", "text/csv;charset=UTF-8"})
    public ResponseEntity<ResourceSupport> uploadCsv(
            @PathVariable @P("submissionId") String submissionId,
            @PathVariable @P("targetType") String targetType,
            @PathVariable @P("repository") String repository,
            @RequestParam @P("templateName") String templateName,
            PersistentEntityResourceAssembler assembler,
            RootResourceInformation resourceInformation,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            InputStream inputStream) throws IOException {

        if (repository == null || !"sheets".equals(repository)) {
            throw new IllegalArgumentException();
        }

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

        //todo validate the sheet!
        //todo process the sheet!
        //todo can we return a projection here?

        sheet.setTemplate(template);

//        sheetHelper.beforeCreate(sheet);

        return persistentEntityCreationHelper.createPersistentEntity(
                sheet,
                resourceInformation,
                assembler,
                acceptHeader
        );


    }
/*
    @RequestMapping(path = "/sheets/{sheetId}/convert", method = RequestMethod.POST)
    public ResponseEntity convertSample(@PathVariable String sheetId)  {

        Sheet sheet = sheetRepository.findOne(sheetId);

        sheetHelper.parse(sheet)
                .map(JSONObject::toString)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, Sample.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }) //ENSURE Aliases set
                .forEach(sample -> sampleRepo.save(sample)); //BEFORE SAVE, AFTER SAVE LIFECYCLE

        return new ResponseEntity(HttpStatus.CREATED);

    }

*/
    /**
     * Consider a command approach here (instead of REST)
     *
     * user commands:
     *  - change header row (
     *  - ignore column
     *  - ignore row
     *  - set this column to that mapping
     *  - change template
     *
     */

}
