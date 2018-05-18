package uk.ac.ebi.subs.api.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class FileDeleteMessage {
    @NonNull
    private String targetFilePath;
    @NonNull
    private String submissionId;
}
