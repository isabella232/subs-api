package uk.ac.ebi.subs.api.controllers;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.converters.ChecklistToSheetConverter;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;

import java.io.IOException;

/**
 * Generating a {@link Spreadsheet} entity from a Checklist MongoDB document.
 * The generated CSV file only contains the headers.
 */
@Data
@RestController
public class SpreadsheetTemplateController {

    @NonNull
    private ChecklistRepository checklistRepository;

    @NonNull
    private ChecklistToSheetConverter templateToSheet;

    @ResponseBody
    @RequestMapping(
            path = "/checklists/{checklistId}/spreadsheet",
            produces = {
                    "text/csv;charset=UTF-8",
                    "text/csv"
            })
    public Spreadsheet templateAsSheet(@PathVariable String checklistId) throws IOException {

        Checklist checklist = checklistRepository.findOne(checklistId);

        if (checklist == null) {
            throw new ResourceNotFoundException();
        }

        Spreadsheet templateSheet = templateToSheet.convert(checklist);

        return templateSheet;

    }

}
