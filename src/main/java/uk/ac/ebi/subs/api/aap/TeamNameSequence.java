package uk.ac.ebi.subs.api.aap;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Tracks the maximum number assigned for using
 */
@Document
@Data
public class TeamNameSequence {

    @Id
    private String teamNamePrefix;
    private int sequenceValue;
}
