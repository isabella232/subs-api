package uk.ac.ebi.subs.api.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class StoredSubmittableDeleteMessage {
    @NonNull
    private String submissionId;
}
