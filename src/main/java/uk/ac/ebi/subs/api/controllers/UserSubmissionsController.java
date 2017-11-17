package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.processors.SubmissionResourceProcessor;
import uk.ac.ebi.subs.api.services.IdentifiableResourceSelfLinker;
import uk.ac.ebi.subs.api.services.UserTeamService;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.projections.SubmissionWithStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@BasePathAwareController
public class UserSubmissionsController {

    private UserTeamService userTeamService;
    private SubmissionRepository submissionRepository;
    private SubmissionStatusRepository submissionStatusRepository;
    private PagedResourcesAssembler<Submission> pagedResourcesAssembler;
    private SpelAwareProxyProjectionFactory projectionFactory;
    private SubmissionResourceProcessor submissionResourceProcessor;
    private IdentifiableResourceSelfLinker<Submission> identifiableResourceSelfLinker;

    public UserSubmissionsController(UserTeamService userTeamService, SubmissionRepository submissionRepository, SubmissionStatusRepository submissionStatusRepository, PagedResourcesAssembler<Submission> pagedResourcesAssembler, SpelAwareProxyProjectionFactory projectionFactory, SubmissionResourceProcessor submissionResourceProcessor, IdentifiableResourceSelfLinker<Submission> identifiableResourceSelfLinker) {
        this.userTeamService = userTeamService;
        this.submissionRepository = submissionRepository;
        this.submissionStatusRepository = submissionStatusRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.projectionFactory = projectionFactory;
        this.submissionResourceProcessor = submissionResourceProcessor;
        this.identifiableResourceSelfLinker = identifiableResourceSelfLinker;
    }

    @RequestMapping("/user/submissions")
    public PagedResources<Resource<SubmissionWithStatus>> getUserSubmissions(Pageable pageable) {
        List<String> userTeamNames = userTeamService.userTeamNames();

        PagedResources<Resource<Submission>> submissionsPagedResource = pagedResourcesAssembler.toResource(
                submissionRepository.findByTeamNameInOrderByCreatedByDesc(userTeamNames, pageable)
        );

        PagedResources.PageMetadata pageMetadata = submissionsPagedResource.getMetadata();
        Collection<Link> pageLinks = submissionsPagedResource.getLinks();

        Collection<Resource<SubmissionWithStatus>> submissionWithStatus = submissionsPagedResource.getContent().stream()
                .map(resource -> identifiableResourceSelfLinker.addSelfLink(resource))
                .map(resource -> submissionResourceProcessor.process(resource))
                .map(resource ->
                        new Resource<>(
                                projectionFactory.createProjection(SubmissionWithStatus.class, resource.getContent()),
                                resource.getLinks()
                        )
                )
                .collect(Collectors.toList());

        return new PagedResources<>(submissionWithStatus, pageMetadata, pageLinks);
    }



    @RequestMapping("/user/submissionStatusSummary")
    public Resource<Map<String, Integer>> getUserSubmissionStatusSummary() {
        List<String> userTeamNames = userTeamService.userTeamNames();
        Map<String, Integer> statusCounts = submissionStatusRepository.submissionStatusCountsByTeam(userTeamNames);

        Link self = linkTo(methodOn(this.getClass()).getUserSubmissionStatusSummary()).withSelfRel();

        return new Resource<>(statusCounts,self);
    }
}
