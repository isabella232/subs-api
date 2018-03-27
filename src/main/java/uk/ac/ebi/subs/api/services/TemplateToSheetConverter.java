package uk.ac.ebi.subs.api.services;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.templates.Template;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TemplateToSheetConverter implements Converter<Template, Sheet> {

    public Sheet convert(Template template) {

        Sheet sheet = new Sheet();
        List<Row> rows = new ArrayList<>();

        Row headerRow = new Row(
                template
                        .getColumnCaptures()
                        .entrySet()
                        .stream()
                        .flatMap(entry -> Stream.concat(
                                Stream.of(entry.getKey()),
                                entry.getValue().expectedColumnHeaders().stream()
                        ))
                        .collect(Collectors.toList())
        );;

        sheet.setHeaderRow(headerRow);
        sheet.setTemplate(template);

        sheet.setSheetName(template.getName() + "_template");

        return sheet;
    }

}
