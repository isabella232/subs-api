package uk.ac.ebi.tsc.aap.client.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Injects a mocked AAP user into the security context.
 */
public class WithMockAAPUserSecurityContextFactory implements WithSecurityContextFactory<WithMockAAPUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockAAPUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Set<Domain> domains = Arrays.stream(annotation.domains())
                .map(domainName -> {
                    Domain domain = new Domain();
                    domain.setDomainName(domainName);
                    return domain;
                }).collect(Collectors.toSet());

        User user = new User(
                annotation.userName(),
                annotation.email(),
                annotation.userReference(),
                domains
        );

        UserAuthentication userAuthentication = new UserAuthentication(user);
        context.setAuthentication(userAuthentication);
        return context;
    }
}
