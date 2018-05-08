package uk.ac.ebi.subs.api.model;

import lombok.Data;

@Data
public class FileDeleteMessage {
    private String targetFilePath;
    private String submissionId;
}
