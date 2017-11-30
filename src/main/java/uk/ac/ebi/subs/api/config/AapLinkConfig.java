package uk.ac.ebi.subs.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("aap.domains")
@Data
public class AapLinkConfig {

    private String url;

}
