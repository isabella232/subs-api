package uk.ac.ebi.subs.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import uk.ac.ebi.tsc.aap.client.security.StatelessAuthenticationEntryPoint;
import uk.ac.ebi.tsc.aap.client.security.StatelessAuthenticationFilter;
import uk.ac.ebi.tsc.aap.client.security.TokenAuthenticationService;

//import uk.ac.ebi.tsc.aap.domains.rest.security.StatelessAuthenticationFilter;
//import uk.ac.ebi.tsc.aap.domains.rest.security.TokenAuthenticationService;

/**
 * Created by ukumbham on 28/09/2016.
 */
@SuppressWarnings("SpringJavaAutowiringInspection")
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan("uk.ac.ebi.tsc.aap.client")
//ComponentScan annotation to scan aap-client-java external artifact
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Autowired
    private StatelessAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private TokenAuthenticationService tokenAuthenticationService;


    private StatelessAuthenticationFilter statelessAuthenticationFilterBean() throws Exception {
        LOGGER.info("this.tokenAuthenticationService: " + this.tokenAuthenticationService);
        return new StatelessAuthenticationFilter(this.tokenAuthenticationService);
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        LOGGER.info("[StatelessAuthenticationEntryPoint]- " + unauthorizedHandler);
        httpSecurity
                // we don't need CSRF because our token is invulnerable
                .csrf().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                // don't create session
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests().anyRequest().authenticated();

        httpSecurity.addFilterBefore(statelessAuthenticationFilterBean(),
                UsernamePasswordAuthenticationFilter.class);
        // disable page caching
        httpSecurity.headers().cacheControl();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService());
    }
}