package uk.ac.ebi.subs.api.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.tsc.aap.client.exception.AAPException;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.Profile;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserTeamService {


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${usi.teamName.prefix}")
    @Setter
    private String teamNamePrefix;

    @NonNull
    private DomainService domainService;

    @NonNull
    private ProfileService profileService;


    public List<Team> userTeams(String userToken) {
        Collection<Domain> domains = domainService.getMyDomains(userToken);

        List<Team> teams = domains.parallelStream()
                .filter(d -> d.getDomainName().startsWith(teamNamePrefix))
                .map(d -> domainToTeam(d, userToken))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return teams;
    }

    private Team domainToTeam(Domain d, String userToken) {
        Team t = Team.build(d.getDomainName());
        t.setDescription(d.getDomainDesc());
        try {
            Profile p = profileService.getDomainProfile(d.getDomainReference(), userToken);

            t.setProfile(p.getAttributes());
        } catch (ResourceAccessException e) {
            logger.warn("failed to get profile for domain", e);
        } catch (AAPException aapException) {
            // We just log this error, because we don't want to throw this error to the user
            // This could happen because the user's added a new domain and forgot to refresh its token
            logger.warn("Failed to get profile information for this domain: {}. The user's token might be outdated and needs to be refreshed.",
                    d.getDomainName());
            return null;
        }

        return t;
    }

    public List<String> userTeamNames() {

        return userTeamNamesStream()
                .collect(Collectors.toList());
    }

    public Stream<String> userTeamNamesStream() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return Stream.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replaceFirst("ROLE_", ""))
                .filter(a -> a.startsWith(teamNamePrefix));
    }
}
