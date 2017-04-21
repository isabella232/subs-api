package uk.ac.ebi.subs.api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.security.TokenAuthenticationService;
import uk.ac.ebi.tsc.aap.client.security.TokenHandler;
import uk.ac.ebi.tsc.aap.client.security.UserAuthentication;
import uk.ac.ebi.tsc.aap.client.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import uk.ac.ebi.tsc.aap.client.repo.DomainService;

/**
 * Created by neilg on 21/04/2017.
 */
@Component
@ComponentScan("uk.ac.ebi.tsc.aap.client")
public class SubsTokenAuthenticationService extends TokenAuthenticationService {

    private static Map<String, UserAuthentication> tokenCacheMap = new ConcurrentHashMap<>();

    @Autowired
    DomainService domainService;

    @Value("${usi.aap.team_prefix}")
    private String usiAppTeamPrefix;

    @Autowired
    public SubsTokenAuthenticationService(TokenHandler tokenHandler) {
        super(tokenHandler);
    }

    @Override
    public Authentication getAuthentication(HttpServletRequest request) {
        final String token = extractToken(request);
        UserAuthentication authentication = null;

        if (tokenCacheMap.get(token) != null) {
            authentication = tokenCacheMap.get(token);
        } else {
            authentication = (UserAuthentication) super.getAuthentication(request);
            getAuthenticationAndCreateDomain(token, authentication);
            tokenCacheMap.put(token, authentication);
        }

        return authentication;
    }

    private void getAuthenticationAndCreateDomain(String token, UserAuthentication authentication) {
        final User user = (User) authentication.getDetails();

        final Collection<? extends GrantedAuthority> usiAuthorities = authentication.getAuthorities()
                .stream()
                .filter(x -> isUSIAuthority(x))
                .collect(Collectors.toList());

        if (usiAuthorities.isEmpty()) {
            final Collection<Domain> domains = domainService.getDomains(user, token);
            Domain newDomain = createNewDomain(user);
            if (!domains.contains(newDomain)) {
                final Domain domain = domainService.createDomain(newDomain.getDomainName(), newDomain.getDomainDesc(), token);
                user.getDomains().add(domain);
            }

        }
    }

    protected boolean isUSIAuthority(GrantedAuthority grantedAuthority) {
        return isUSIAuthority(grantedAuthority, usiAppTeamPrefix);
    }

    protected boolean isUSIAuthority(GrantedAuthority grantedAuthority, String authorityPrefix) {
        return grantedAuthority.getAuthority().toUpperCase().startsWith(authorityPrefix);
    }

    protected Domain createNewDomain(User user) {
        return new Domain(usiAppTeamPrefix + user.getUsername(), "USI Domain", null);
    }


}
