package uk.ac.ebi.subs.api.sheetloader;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.repository.security.RoleLookup;
import uk.ac.ebi.tsc.aap.client.model.Domain;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AdminUserService {

    @NonNull
    private RoleLookup roleLookup;

    public void injectAdminUserIntoSecurityContext(){
        Domain domain = new Domain();
        domain.setDomainName(roleLookup.adminRole());

        Authentication auth = new UsernamePasswordAuthenticationToken("subs-processor", "", Arrays.asList(domain) );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
