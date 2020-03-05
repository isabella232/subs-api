package uk.ac.ebi.subs.api.model.fileupload.globus;

import lombok.Data;

import java.util.List;

@Data
public class GlobusUploadedFilesNotification {

    private String owner;

    private String submissionId;

    private List<String> files;
}
