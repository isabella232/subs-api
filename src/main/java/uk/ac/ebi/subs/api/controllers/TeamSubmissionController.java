package uk.ac.ebi.subs.api.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.converters.SubmissionDTOConverter;
import uk.ac.ebi.subs.api.processors.SubmissionResourceProcessor;
import uk.ac.ebi.subs.api.services.UserTokenService;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.security.PreAuthorizeParamTeamName;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.Profile;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileService;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;


/**
 * This controller accepts new Submissions for a team.
 * Unlike the SubmissionContentsController, it can't be implemented using Spring Data Rest
 * repository / persistent entity handling. SDR requires that the top level entity type is
 * exported through SDR, which isn't possible for teams.
 * We have therefore reimplemented some functionality from SDR.
 */
@RestController
@RequiredArgsConstructor
public class TeamSubmissionController {

    @NonNull private SubmissionRepository submissionRepository;
    @NonNull private ApplicationEventPublisher publisher;
    @NonNull private RepositoryRestConfiguration config;
    @NonNull private RepositoryEntityLinks repositoryEntityLinks;
    @NonNull private SubmissionResourceProcessor submissionResourceProcessor;
    @NonNull private ProfileService profileService;
    @NonNull private DomainService domainService;
    @NonNull private UserTokenService userTokenService;
    @NonNull private SubmissionDTOConverter submissionDTOConverter;


    @PreAuthorizeParamTeamName
    @RequestMapping(value = "/teams/{teamName:.+}/submissions", method = RequestMethod.POST)
    public ResponseEntity<RepresentationModel<?>> createTeamSubmission(
            @PathVariable @P("teamName") String teamName,
            @RequestBody SubmissionDTO submissionDTO,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @RequestHeader("Authorization") String authorizationHeader

    ) {
        Team team = getTeamFromAuthHeader(authorizationHeader, teamName);

        if (team == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Submission submission = submissionDTOConverter.convert(submissionDTO);
        submission.setTeam(team);

        Submission savedSubmission = createSubmission(submission);

        EntityModel<Submission> resource = buildSubmissionResource(submission, savedSubmission);

        HttpHeaders httpHeaders = buildHeaders(resource, savedSubmission);

        return buildResponseEntity(acceptHeader, resource, httpHeaders);
    }

    private Team getTeamFromAuthHeader(String authorizationHeader, String teamName) {
        String token = userTokenService.authorizationHeaderValueToToken(authorizationHeader);
        Collection<Domain> userDomains = domainService.getMyDomains(token);
        Optional<Domain> domainOptional = userDomains.stream().filter(d -> teamName.equals(d.getDomainName())).findAny();

        if (domainOptional.isEmpty()) {
            // possible to reach this if the user does not currently belong to the domain in AAP
            return null;
        }

        Domain domain = domainOptional.get();
        Profile profile = profileService.getDomainProfile(domain.getDomainReference(), token);
        Team team = new Team();
        team.setName(teamName);
        team.setDescription(domain.getDomainDesc());

        Map<String,String> attributes = Collections.emptyMap();

        if (profile != null && profile.getAttributes() != null){
            attributes = profile.getAttributes();
        }

        // profile requirements are checked in the team validator
        team.setProfile(attributes);

        return team;
    }

    private Submission createSubmission(@RequestBody Submission submission) {
        publisher.publishEvent(new BeforeCreateEvent(submission));
        Submission savedSubmission = submissionRepository.insert(submission);
        publisher.publishEvent(new AfterCreateEvent(savedSubmission));
        return savedSubmission;
    }

    private ResponseEntity<RepresentationModel<?>> buildResponseEntity(
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            EntityModel<Submission> resource, HttpHeaders httpHeaders) {
        boolean returnBody = config.returnBodyOnCreate(acceptHeader);

        if (returnBody) {
            return ControllerUtils.toResponseEntity(
                    HttpStatus.CREATED,
                    httpHeaders,
                    resource
            );
        } else {
            return ControllerUtils.toEmptyResponse(
                    HttpStatus.CREATED,
                    httpHeaders
            );
        }
    }

    private EntityModel<Submission> buildSubmissionResource(@RequestBody Submission submission, Submission savedSubmission) {
        EntityModel<Submission> resource = new EntityModel<>(savedSubmission);
        resource.add(
                repositoryEntityLinks.linkToItemResource(submission.getClass(), submission.getId())
        );
        resource.add(
                repositoryEntityLinks.linkToItemResource(submission.getClass(), submission.getId()).withSelfRel()
        );
        final SubmissionStatus submissionStatus = submission.getSubmissionStatus();
        resource.add(
                repositoryEntityLinks.linkToItemResource(submissionStatus.getClass(), submissionStatus.getId())
        );

//        resource = submissionResourceProcessor.process(resource);
        return resource;
    }

    private HttpHeaders buildHeaders(EntityModel<Submission> resource, Submission submission) {
        HttpHeaders httpHeaders = new HttpHeaders();

        ETag.from(submission.getVersion().toString()).addTo(httpHeaders);

        httpHeaders.setLastModified(submission.getCreatedDate().getTime());

        if (resource.getLink("self").isPresent()) {
            URI selfLink = URI.create(resource.getLink("self").get().expand().getHref());
            httpHeaders.setLocation(selfLink);
        }

        return httpHeaders;
    }
}
