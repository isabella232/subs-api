package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.security.access.method.P;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.security.PreAuthorizeParamTeamName;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@BasePathAwareController
public class TeamController {

    private SubmissionRepository submissionRepository;
    private PagedResourcesAssembler<Team> teamPagedResourcesAssembler;
    public TeamController(SubmissionRepository submissionRepository,
                          PagedResourcesAssembler<Team> teamPagedResourcesAssembler) {
        this.submissionRepository = submissionRepository;
        this.teamPagedResourcesAssembler = teamPagedResourcesAssembler;
    }

    @RequestMapping("/teams")
    public Resources<Resource<Team>> getTeams(Pageable pageable) {

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Team> teamList = new ArrayList<>();

        if (authentication != null && authentication.getAuthorities().size() > 0) {
            teamList = authentication.getAuthorities().stream().map(a -> {
                Team team = new Team();
                team.setName(a.getAuthority().toString().replaceFirst("ROLE_", ""));
                return team;
            }).collect(Collectors.toList());

        }

        final PageImpl<Team> teams = new PageImpl<>(teamList, pageable, authentication.getAuthorities().size());
        return teamPagedResourcesAssembler.toResource(teams);
    }


    @RequestMapping("/teams/{teamName:.+}")
    @PreAuthorizeParamTeamName
    public Resource<Team> getTeam(@PathVariable @P("teamName") String teamName) {

        Team d = new Team();
        d.setName(teamName);

        Resource<Team> resource = new Resource<>(d);

        resource.add(
                linkTo(
                        methodOn(this.getClass()).getTeam(
                                d.getName()
                        )
                ).withSelfRel()
        );

        return resource;
    }



}
