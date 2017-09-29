package uk.ac.ebi.subs.api.controllers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.*;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.subs.api.processors.SubmissionResourceProcessor;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.security.PreAuthorizeParamTeamName;

import java.net.URI;

@RestController
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

        publisher.publishEvent(new BeforeCreateEvent(submission));
        Submission savedSubmission = submissionRepository.insert(submission);
        publisher.publishEvent(new AfterCreateEvent(savedSubmission));


        Resource<Submission> resource = new Resource<>(savedSubmission);
        resource.add(
                repositoryEntityLinks.linkToSingleResource(submission)
        );
        resource.add(
                repositoryEntityLinks.linkToSingleResource(submission).withSelfRel()
        );

        resource = submissionResourceProcessor.process(resource);


        HttpHeaders httpHeaders = buildHeaders(resource,savedSubmission);

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
