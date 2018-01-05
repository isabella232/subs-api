package uk.ac.ebi.subs.api.handlers;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.api.services.SheetService;
import uk.ac.ebi.subs.repository.model.sheets.Sheet;
import uk.ac.ebi.subs.repository.model.sheets.SheetStatusEnum;

@Component
@Data
@RepositoryEventHandler(Sheet.class)
public class SheetHandler {

    @NonNull
    private SheetService sheetService;

    @HandleBeforeCreate
    public void handleBeforeCreate(Sheet sheet){
        //pre-process the sheet, run column mapping
        sheetService.fillInId(sheet);
        sheet.setStatus(SheetStatusEnum.Draft);
        Assert.notNull(sheet.getTemplate());
        sheet.removeEmptyRows();
        sheet.removeColumnsPastLastNonEmpty();
        sheetService.ignoreCommentLines(sheet);
        sheetService.guessHeader(sheet);
        sheetService.mapHeadings(sheet);
    }

    @HandleAfterSave
    public void handleAfterSave(Sheet sheet){
        //if the status has changed, send an event
        //TODO in SUBS-1037
    }


}
