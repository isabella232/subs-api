package uk.ac.ebi.subs.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.ac.ebi.tsc.aap.client.model.User;

@Configuration
@EnableMongoAuditing(auditorAwareRef = "auditorProvider")
public class RestRepositoryConfig {

    @Bean
    public SpelAwareProxyProjectionFactory projectionFactory() {
        return new SpelAwareProxyProjectionFactory();
    }

    public static final String DEFAULT_USI_USER = "usi-user";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAware<String>() {
            @Override
            public String getCurrentAuditor() {
                final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null) {

                    final Object details = authentication.getDetails();
                    if (details instanceof User) {
                        return ((User) details).getUserReference();
                    } else {
                        return authentication.getName();
                    }

                } else {
                    return DEFAULT_USI_USER;
                }

            }
        };
    }

}