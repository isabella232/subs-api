package uk.ac.ebi.subs.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
// "aap.domains" property comes from the aap library.
// We provide this as an endpoint for aap in our API. It is reconfigurable in the application yml files.
@ConfigurationProperties("aap.domains")
@Data
public class AapLinkConfig {

    private String url;

}
