package uk.ac.ebi.subs.api.controllers;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.subs.api.services.TemplateToSheetConverter;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.templates.Template;
import uk.ac.ebi.subs.repository.repos.TemplateRepository;

import java.io.IOException;

@Data
@Controller
public class TemplateController {

    @NonNull
    private TemplateRepository templateRepository;

    @NonNull
    private TemplateToSheetConverter templateToSheet;

    @ResponseBody
    @RequestMapping(
            path = "/templates/{templateId}/sheet",
            produces = {
                    "text/csv;charset=UTF-8",
                    "text/csv"
            })
    public Sheet templateAsSheet(@PathVariable String templateId) throws IOException {

        Template template = templateRepository.findOne(templateId);

        if (template == null) {
            throw new ResourceNotFoundException();
        }

        Sheet templateSheet = templateToSheet.convert(template);

        return templateSheet;

    }

}
