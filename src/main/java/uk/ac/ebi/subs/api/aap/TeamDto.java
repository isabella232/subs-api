package uk.ac.ebi.subs.api.aap;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
public class TeamDto {

    private String description;

    @NotNull
    @Size(min = 2, max = 500)
    private String centreName;

}
