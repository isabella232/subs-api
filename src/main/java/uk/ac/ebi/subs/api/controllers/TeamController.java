package uk.ac.ebi.subs.api.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.aap.TeamCreationService;
import uk.ac.ebi.subs.api.aap.TeamDto;
import uk.ac.ebi.subs.api.processors.TeamResourceProcessor;
import uk.ac.ebi.subs.api.services.UserTeamService;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.security.PreAuthorizeParamTeamName;
import uk.ac.ebi.tsc.aap.client.model.User;

import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@BasePathAwareController
@RequiredArgsConstructor
public class TeamController {

    @NonNull
    private SubmissionRepository submissionRepository;

    @NonNull
    private PagedResourcesAssembler<Team> teamPagedResourcesAssembler;

    @NonNull
    private UserTeamService userTeamService;

    @NonNull
    private TeamCreationService teamCreationService;

    @NonNull
    private TeamResourceProcessor teamResourceProcessor;

    @RequestMapping("/user/teams")
    public Resources<Resource<Team>> getTeams(Pageable pageable) {

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Team> teamList = userTeamService.userTeams();

        final PageImpl<Team> teams = new PageImpl<>(teamList, pageable, authentication.getAuthorities().size());
        return teamPagedResourcesAssembler.toResource(teams);
    }


    @RequestMapping("/teams/{teamName:.+}")
    @PreAuthorizeParamTeamName
    public Resource<Team> getTeam(@PathVariable @P("teamName") String teamName) {

        Team d = new Team();
        d.setName(teamName);

        Resource<Team> resource = new Resource<>(d);

        addSelfLink(d, resource);

        return resource;
    }

    private void addSelfLink(Team d, Resource<Team> resource) {
        resource.add(
                linkTo(
                        methodOn(this.getClass()).getTeam(
                                d.getName()
                        )
                ).withSelfRel()
        );
    }

    @RequestMapping(value = "/user/teams", method = RequestMethod.POST)
    public ResponseEntity<Resource<Team>> createTeam(@RequestBody TeamDto teamDto) {

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getDetails();


        Team team = teamCreationService.createTeam(user,teamDto);

        Resource<Team> teamResource = new Resource<>(team);

        addSelfLink(team,teamResource);


       return new ResponseEntity<>(
                teamResourceProcessor.process(teamResource),
                HttpStatus.CREATED
        );
    }



}
