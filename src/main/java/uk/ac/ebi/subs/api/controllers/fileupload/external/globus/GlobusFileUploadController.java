package uk.ac.ebi.subs.api.controllers.fileupload.external.globus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.services.FileUploadService;
import uk.ac.ebi.tsc.aap.client.model.User;

import java.security.Principal;

@RestController
public class GlobusFileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping(value = "/fileupload/globus/share")
    public ResponseEntity<GlobusShareResponse> getGlobusShare(Authentication authentication) {
        String email = null;

        Object details = authentication.getDetails();
        if (details instanceof User) {
            User user = (User) details;
            email = user.getEmail();
        }

        if (email == null || email.isBlank()) {
            throw new RuntimeException("User email not available.");
        }

        GlobusShareResponse resp = new GlobusShareResponse();
        resp.setShareLink(fileUploadService.createGlobusShare(email));

        return ResponseEntity.ok(resp);
    }
}
