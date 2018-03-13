package uk.ac.ebi.subs.api.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.aap.TeamDto;
import uk.ac.ebi.subs.api.aap.TeamNameSequenceService;
import uk.ac.ebi.subs.api.aap.UsiTokenService;
import uk.ac.ebi.subs.api.processors.TeamResourceProcessor;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.TokenService;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class TeamCreationController {

    @NonNull
    private TeamNameSequenceService teamNameSequenceService;

    @NonNull
    private UsiTokenService usiTokenService;

    @NonNull
    private DomainService domainService;

    @NonNull
    private TeamResourceProcessor teamResourceProcessor;

    @RequestMapping(value = "/user/teams", method = RequestMethod.POST)
    public Resource<Team> createTeam(@RequestBody TeamDto teamDto) {

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getDetails();


        String description = teamDto.getDescription();
        if (description == null) {
            description = "";
        }

        String teamName = teamNameSequenceService.nextTeamName();

        Domain domain = domainService.createDomain(teamName, description, usiTokenService.aapToken());
        domainService.addUserToDomain(domain, user, usiTokenService.aapToken());
        domainService.addManagerToDomain(domain,user,usiTokenService.aapToken());

        //TODO remove USI as domain manager

        Team team = new Team();
        team.setName(domain.getDomainName());

        Resource<Team> teamResource = new Resource<>(team);

        return teamResourceProcessor.process(teamResource);
    }


}
