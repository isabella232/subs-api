package uk.ac.ebi.subs.api.model.fileupload.globus;

import lombok.Data;

@Data
public class GlobusShareRequest {
    private String owner;

    private String submissionId;
}
