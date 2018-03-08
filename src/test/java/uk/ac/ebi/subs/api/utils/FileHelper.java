package uk.ac.ebi.subs.api.utils;

import uk.ac.ebi.subs.repository.model.fileupload.File;

import java.util.UUID;

public class FileHelper {

    private static final String TUS_ID = UUID.randomUUID().toString();
    private static final String FILENAME = "test.cram";
    private static final long TOTAL_SIZE = 123456L;


    public static File createFile(String submissionId) {
        File file = new File();
        file.setGeneratedTusId(TUS_ID);
        file.setSubmissionId(submissionId);
        file.setFilename(FILENAME);
        file.setTotalSize(TOTAL_SIZE);

        return file;
    }

}
