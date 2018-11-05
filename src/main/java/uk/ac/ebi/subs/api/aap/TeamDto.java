package uk.ac.ebi.subs.api.aap;

import lombok.Builder;
import lombok.Data;

/**
 * This value object contains the description and centre name of a team.
 */
@Data
@Builder
public class TeamDto {

    private String description;

    private String centreName;

}
