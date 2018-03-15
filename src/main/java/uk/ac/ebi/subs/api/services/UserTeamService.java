package uk.ac.ebi.subs.api.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.data.component.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserTeamService {

    @Value("${usi.teamName.prefix}")
    private String teamNamePrefix;

    public List<Team> userTeams() {

        return userTeamNamesStream()
                .map(a -> Team.build(a))
                .collect(Collectors.toList());
    }

    public List<String> userTeamNames() {

        return userTeamNamesStream()
                .collect(Collectors.toList());
    }

    public Stream<String> userTeamNamesStream() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null){
            return Stream.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replaceFirst("ROLE_", ""))
                .filter(a -> a.startsWith(teamNamePrefix));
    }
}
