package uk.ac.ebi.subs.api.aap;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;

@Service
@RequiredArgsConstructor
public class TeamCreationService {

    @NonNull
    private TeamNameSequenceService teamNameSequenceService;

    @NonNull
    private UsiTokenService usiTokenService;

    @NonNull
    private DomainService domainService;

    public Team createTeam(User user, TeamDto teamDto){
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

        return team;
    }
}
