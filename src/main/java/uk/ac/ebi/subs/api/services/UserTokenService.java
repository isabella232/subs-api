package uk.ac.ebi.subs.api.services;

import org.springframework.stereotype.Component;

/**
 * This is a helper component to get the JWT token value from the authorization header string.
 */
@Component
public class UserTokenService {

    public String authorizationHeaderValueToToken(String authorizationHeader){
        return authorizationHeader.replaceFirst("Bearer ", "");
    }
}
