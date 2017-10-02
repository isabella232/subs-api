package uk.ac.ebi.tsc.aap.client.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;

import java.util.Arrays;
import java.util.stream.Collectors;

public class WithMockAAPUserSecurityContextFactory implements WithSecurityContextFactory<WithMockAAPUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockAAPUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        User user = new User();
        user.setEmail(annotation.email());
        user.setUserReference(annotation.userReference());
        user.setUserName(annotation.userName());
        user.setDomains( Arrays.stream(annotation.domains())
                .map(domainName -> {
                    Domain domain = new Domain();
                    domain.setDomainName(domainName);
                    return domain;
                }).collect(Collectors.toSet()));
        UserAuthentication userAuthentication = new UserAuthentication(user);
        context.setAuthentication(userAuthentication);
        return context;
    }
}
