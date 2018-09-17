package uk.ac.ebi.subs.api.converters;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.model.sheets.Row;
import uk.ac.ebi.subs.repository.model.sheets.Spreadsheet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Component
public class SheetCsvMessageConverter extends AbstractHttpMessageConverter<Spreadsheet> {

    public static final MediaType CSV_UTF8_MEDIA_TYPE = new MediaType("text", "csv", Charset.forName("utf-8"));
    public static final MediaType CSV_MEDIA_TYPE = new MediaType("text", "csv");

    public SheetCsvMessageConverter() {
        super(CSV_MEDIA_TYPE, CSV_UTF8_MEDIA_TYPE);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Spreadsheet.class.equals(clazz);
    }


    @Override
    protected void writeInternal(Spreadsheet sheet, HttpOutputMessage output) throws IOException, HttpMessageNotWritableException {
        output.getHeaders().setContentType(CSV_UTF8_MEDIA_TYPE);
        output.getHeaders().set("Content-Disposition", "attachment; filename=\"" + sheet.getSheetName() + ".csv\"");

        OutputStream outputStream = output.getBody();

        Writer out = new BufferedWriter(new OutputStreamWriter(outputStream));
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL);

        printer.printRecord(
                sheet.getHeaderRow().getCells()
        );

        for (Row row : sheet.getRows()) {
            printer.printRecord(row.getCells());
        }

        printer.close();
    }

    @Override
    protected Spreadsheet readInternal(Class<? extends Spreadsheet> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return readStream(inputMessage.getBody());

    }

    public Spreadsheet readStream(InputStream inputStream) throws IOException {
        Spreadsheet sheet = new Spreadsheet();

        Reader in = new InputStreamReader(inputStream);
        CSVParser csvParser = CSVFormat.EXCEL.parse(in);


        for (CSVRecord record : csvParser) {
            List<String> row = new ArrayList<>(record.size());
            record.forEach(row::add);

            sheet.addRow(row);
        }

        return sheet;
    }
}