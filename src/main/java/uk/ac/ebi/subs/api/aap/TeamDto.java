package uk.ac.ebi.subs.api.aap;

/**
 * This value object contains the description and centre name of a team.
 */
public class TeamDto {

    private String description;

    private String centreName;

    public TeamDto() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCentreName() {
        return centreName;
    }

    public void setCentreName(String centreName) {
        this.centreName = centreName;
    }
}
