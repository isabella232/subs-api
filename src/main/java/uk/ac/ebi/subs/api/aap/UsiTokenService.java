package uk.ac.ebi.subs.api.aap;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import uk.ac.ebi.tsc.aap.client.repo.TokenService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * Provides a single token for USI. Checks, if the token is set and if it would expire in 5 minutes.
 * If it would expire, then gets a new token from the {@link TokenService} service.
 */
@Component
@RequiredArgsConstructor
public class UsiTokenService {

    private static final Logger logger = LoggerFactory.getLogger(UsiTokenService.class);

    @Value("${aap.domains.url}/auth")
    private String aapUri;

    @Value("${usi.tokenservice.username}")
    private String username;

    @Value("${usi.tokenservice.password}")
    private String password;
    private Logger log = LoggerFactory.getLogger(getClass());

    private Optional<String> jwt = Optional.empty();
    private Optional<Date> expiry = Optional.empty();

    @NonNull
    private TokenService tokenService;

    public synchronized String aapToken() {
        log.debug("JWT requested");
        if (username == null || username.trim().length() == 0
                || password == null || password.trim().length() == 0) {
            return null;
        }

        if (isFreshTokenRequired()) {
            log.debug("Fetching fresh JWT");
            jwt = Optional.of(tokenService.getAAPToken(username,password));

            try {
                DecodedJWT decodedJwt = JWT.decode(jwt.get());
                Optional<Date> tokenExpiry = Optional.of(decodedJwt.getExpiresAt());

                expiry = shortenTokenLifetime(tokenExpiry);

            } catch (JWTDecodeException e) {
                //Invalid token
                throw new RuntimeException(e);
            }

            log.debug("Fresh JWT obtained, expires {}, {}", expiry, jwt);
        }

        return jwt.get();
    }

    private Optional<Date> shortenTokenLifetime(Optional<Date> tokenExpiry) {
        if (!tokenExpiry.isPresent()) {
            return tokenExpiry;
        }

        Date expiryTime = tokenExpiry.get();
        Date fiveMinutesEarlier = new Date(expiryTime.getTime() - FIVE_MINS_IN_MILLIS);

        return Optional.of(fiveMinutesEarlier);
    }

    private static final long FIVE_MINS_IN_MILLIS = 5 * 60 * 1000;


    private boolean isFreshTokenRequired() {
        Date now = new Date();
        return !jwt.isPresent() || (expiry.isPresent() && expiry.get().before(now));
    }
}

