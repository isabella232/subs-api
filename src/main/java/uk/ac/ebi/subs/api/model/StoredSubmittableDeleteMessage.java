package uk.ac.ebi.subs.api.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Value object used for sending a {@link uk.ac.ebi.subs.repository.model.StoredSubmittable} deletion message.
 */
@Data
@RequiredArgsConstructor
public class StoredSubmittableDeleteMessage {
    @NonNull
    private String submissionId;
}
