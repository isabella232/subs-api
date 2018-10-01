package uk.ac.ebi.subs.api.services;

import org.springframework.stereotype.Component;

@Component
public class UserTokenService {

    public String authorizationHeaderValueToToken(String authorizationHeader){
        return authorizationHeader.replaceFirst("Bearer ", "");
    }
}
