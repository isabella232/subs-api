package uk.ac.ebi.subs.api.services;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.data.component.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserTeamService {

    public List<Team> userTeams() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Team> teamList = new ArrayList<>();

        if (authentication != null && authentication.getAuthorities().size() > 0) {
            teamList = authentication.getAuthorities().stream().map(a -> {
                Team team = new Team();
                team.setName(a.getAuthority().toString().replaceFirst("ROLE_", ""));
                return team;
            }).collect(Collectors.toList());

        }
        return teamList;
    }
}
