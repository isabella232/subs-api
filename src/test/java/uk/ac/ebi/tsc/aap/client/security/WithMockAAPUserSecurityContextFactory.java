package uk.ac.ebi.tsc.aap.client.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import uk.ac.ebi.tsc.aap.client.model.User;

/**
 * Injects a mocked AAP user into the security context.
 */
public class WithMockAAPUserSecurityContextFactory implements WithSecurityContextFactory<WithMockAAPUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockAAPUser annotation) {
        User user = User.builder()
                .withReference(annotation.userReference())
                .withUsername(annotation.userName())
                .withEmail(annotation.email())
                .withFullName(annotation.fullName())
                .withDomains(annotation.domains())
                .build();

        return this.setUserInSecurityContext(user);
    }

    public static SecurityContext setUserInSecurityContext(User user) {
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(new UserAuthentication(user));
        return sc;
    }
}
