package uk.ac.ebi.subs.api.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import uk.ac.ebi.tsc.aap.client.model.User;

import java.util.List;

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