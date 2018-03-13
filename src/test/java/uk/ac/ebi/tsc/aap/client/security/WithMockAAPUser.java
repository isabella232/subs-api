package uk.ac.ebi.tsc.aap.client.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * When used with {@link WithMockAAPUserSecurityContextFactory} this annotation can be
 * added to a test method to emulate running with a mocked AAP user.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockAAPUserSecurityContextFactory.class)
public @interface WithMockAAPUser {

    /**
     * The AAP username
     * @return
     */
    String userName();

    /**
     * The AAP email address
     * @return
     */
    String email();

    /**
     * The AAP user reference
     * @return
     */
    String userReference();

    /**
     * The AAP full name
     * @return
     */
    String fullName();


    /**
     * The AAP domains / roles
     * @return
     */
    String[] domains();
}
