package uk.ac.ebi.subs.api.controllers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
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
import uk.ac.ebi.subs.api.processors.SubmissionResourceProcessor;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.security.PreAuthorizeParamTeamName;

import java.net.URI;

@RestController
/**
 * This controller accepts new Submissions for a team.
 * Unlike the SubmissionContentsController, it can't be implemented using Spring Data Rest
 * repository / persistent entity handling. SDR requires that the top level entity type is
 * exported through SDR, which isn't possible for teams.
 * We have therefore reimplemented some functionality from SDR.
 */
public class TeamSubmissionController {

    private SubmissionRepository submissionRepository;
    private ApplicationEventPublisher publisher;
    private RepositoryRestConfiguration config;
    private RepositoryEntityLinks repositoryEntityLinks;
    private SubmissionResourceProcessor submissionResourceProcessor;

    public TeamSubmissionController(SubmissionRepository submissionRepository, ApplicationEventPublisher publisher, RepositoryRestConfiguration config, RepositoryEntityLinks repositoryEntityLinks, SubmissionResourceProcessor submissionResourceProcessor) {
        this.submissionRepository = submissionRepository;
        this.publisher = publisher;
        this.config = config;
        this.repositoryEntityLinks = repositoryEntityLinks;
        this.submissionResourceProcessor = submissionResourceProcessor;
    }

    @PreAuthorizeParamTeamName
    @RequestMapping(value = "/teams/{teamName:.+}/submissions", method = RequestMethod.POST)
    public ResponseEntity<ResourceSupport> createTeamSubmission(
            @PathVariable @P("teamName") String teamName,
            @RequestBody Submission submission,
            @RequestHeader(value = "Accept", required = false) String acceptHeader
    ) {

        submission.setTeam(Team.build(teamName));

        Submission savedSubmission = createSubmission(submission);

        Resource<Submission> resource = buildSubmissionResource(submission, savedSubmission);

        HttpHeaders httpHeaders = buildHeaders(resource,savedSubmission);

        return buildResponseEntity(acceptHeader, resource, httpHeaders);
    }

    private Submission createSubmission(@RequestBody Submission submission) {
        publisher.publishEvent(new BeforeCreateEvent(submission));
        Submission savedSubmission = submissionRepository.insert(submission);
        publisher.publishEvent(new AfterCreateEvent(savedSubmission));
        return savedSubmission;
    }

    private ResponseEntity<ResourceSupport> buildResponseEntity(@RequestHeader(value = "Accept", required = false) String acceptHeader, Resource<Submission> resource, HttpHeaders httpHeaders) {
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

    private Resource<Submission> buildSubmissionResource(@RequestBody Submission submission, Submission savedSubmission) {
        Resource<Submission> resource = new Resource<>(savedSubmission);
        resource.add(
                repositoryEntityLinks.linkToSingleResource(submission)
        );
        resource.add(
                repositoryEntityLinks.linkToSingleResource(submission).withSelfRel()
        );
        resource.add(
                repositoryEntityLinks.linkToSingleResource(submission.getSubmissionStatus())
        );

        resource = submissionResourceProcessor.process(resource);
        return resource;
    }

    private HttpHeaders buildHeaders(Resource<Submission> resource, Submission submission) {
        HttpHeaders httpHeaders = new HttpHeaders();

        ETag.from(submission.getVersion().toString()).addTo(httpHeaders);

        httpHeaders.setLastModified(submission.getCreatedDate().getTime());

        if (resource.getLink("self") != null) {
            URI selfLink = URI.create(resource.getLink("self").expand().getHref());
            httpHeaders.setLocation(selfLink);
        }

        return httpHeaders;
    }
}
