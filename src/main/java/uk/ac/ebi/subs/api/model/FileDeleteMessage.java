package uk.ac.ebi.subs.api.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Value object used for sending a {@link uk.ac.ebi.subs.repository.model.fileupload.File} deletion message.
 */
@RequiredArgsConstructor
@Data
public class FileDeleteMessage {
    @NonNull
    private String targetFilePath;
    @NonNull
    private String submissionId;
}
