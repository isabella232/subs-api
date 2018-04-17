package uk.ac.ebi.subs.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("usi.tus-upload")
@Data
public class TusUploadConfig {

    private String url;
}
