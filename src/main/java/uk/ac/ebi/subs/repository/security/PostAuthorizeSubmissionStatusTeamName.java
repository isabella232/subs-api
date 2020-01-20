package uk.ac.ebi.subs.repository.security;

import org.springframework.security.access.prepost.PostAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PostAuthorize("returnObject.isEmpty() or hasAnyRole(@roleLookup.adminRole(),@teamNameExtractor.submissionStatusTeam(returnObject.get()))")
public @interface PostAuthorizeSubmissionStatusTeamName {
}
