package uk.ac.ebi.subs.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
// We provide this property as an endpoint for file upload in our API. It is reconfigurable in the application yml files.
@ConfigurationProperties("usi.tus-upload")
@Data
public class TusUploadConfig {

    private String url;
}
