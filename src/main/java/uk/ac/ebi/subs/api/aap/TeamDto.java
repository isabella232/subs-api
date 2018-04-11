package uk.ac.ebi.subs.api.aap;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamDto {

    private String description;

    private String centreName;

}
