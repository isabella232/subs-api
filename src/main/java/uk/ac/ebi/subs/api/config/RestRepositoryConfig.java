package uk.ac.ebi.subs.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;

@Configuration
@EnableMongoAuditing(auditorAwareRef="auditorProvider")
public class RestRepositoryConfig {


    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer() {

        return new RepositoryRestConfigurerAdapter() {

            @Override
            public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
                config.setRepositoryDetectionStrategy(
                        RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED
                );
            }
        };
    }

    @Bean
    public AuditorAware<String> auditorProvider(){
        return new AuditorAware<String>() {
            @Override
            public String getCurrentAuditor() {
                return "nemo"; //TODO once authentication is working, make this return the real user
            }
        };
    }



}