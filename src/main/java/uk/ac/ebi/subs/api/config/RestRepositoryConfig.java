package uk.ac.ebi.subs.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.ac.ebi.tsc.aap.client.model.User;

import java.util.Optional;

/**
 * Used by Spring. Tells to Spring the user that is calling the Spring Data REST method.
 */
@Configuration
@EnableMongoAuditing(auditorAwareRef = "auditorProvider")
public class RestRepositoryConfig {

    @Bean
    public SpelAwareProxyProjectionFactory projectionFactory() {
        return new SpelAwareProxyProjectionFactory();
    }

    private static final String DEFAULT_USI_USER = "usi-user";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {

                final Object details = authentication.getDetails();
                if (details instanceof User) {
                    return Optional.ofNullable(((User) details).getUserReference());
                } else {
                    return Optional.ofNullable(authentication.getName());
                }
            } else {
                return Optional.of(DEFAULT_USI_USER);
            }
        };
    }
}
