package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.processors.LinkHelper;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.security.PreAuthorizeParamTeamName;

@RestController
@BasePathAwareController
public class TeamItemsController implements ResourceProcessor<TeamItemsController.TeamItemsResource> {

    private LinkHelper linkHelper;

    public TeamItemsController(LinkHelper linkHelper) {
        this.linkHelper = linkHelper;
    }

    @PreAuthorizeParamTeamName
    @RequestMapping("/teams/{teamName}/items")
    public TeamItemsResource TeamItems(@PathVariable @P("teamName") String teamName) {

        Team team = Team.build(teamName);

        return new TeamItemsResource(team);

    }

    @Override
    public TeamItemsResource process(TeamItemsResource resource) {
        linkHelper.addSubmittablesInTeamLinks(resource.getLinks(), resource.team.getName());

        resource.team = null;

        return resource;
    }

    class TeamItemsResource extends ResourceSupport {
        private Team team;

        public TeamItemsResource(Team team) {
            this.team = team;
        }
    }
}
