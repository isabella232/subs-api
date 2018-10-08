package uk.ac.ebi.subs.api.services;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;
import uk.ac.ebi.subs.repository.model.templates.Template;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ChecklistToSheetConverter implements Converter<Checklist, Spreadsheet> {

    public Spreadsheet convert(Checklist checklist) {

        Spreadsheet sheet = new Spreadsheet();
        Template template = checklist.getSpreadsheetTemplate();

        Row headerRow = new Row(
                template
                        .getColumnCaptures()
                        .entrySet()
                        .stream()
                        .flatMap(entry -> Stream.concat(
                                Stream.of(entry.getKey()),
                                entry.getValue().additionalExpectedColumnHeaders().stream()
                        ))
                        .collect(Collectors.toList())
        );
        ;

        sheet.setHeaderRow(headerRow);
        sheet.setChecklistId(checklist.getId());

        sheet.setSheetName(checklist.getId() + "_template");

        return sheet;
    }

}
