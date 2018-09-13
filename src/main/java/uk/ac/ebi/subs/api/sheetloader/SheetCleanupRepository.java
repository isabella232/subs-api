package uk.ac.ebi.subs.api.sheetloader;

import uk.ac.ebi.subs.repository.repos.SpreadsheetRepository;

import java.util.Date;

public interface SheetCleanupRepository extends SpreadsheetRepository {


    void removeByLastModifiedDateBeforeAndStatus(Date lastModifiedBy, String status);
}
