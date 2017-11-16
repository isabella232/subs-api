package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.services.UserTeamService;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.projections.SubmissionWithStatus;
import uk.ac.ebi.subs.repository.projections.SubmittableWithStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;

import java.util.List;
import java.util.Map;

@RestController
@BasePathAwareController
public class UserItemsController {

    private UserTeamService userTeamService;
    private ProjectRepository projectRepository;
    private SubmissionRepository submissionRepository;
    private SubmissionStatusRepository submissionStatusRepository;
    private PagedResourcesAssembler pagedResourcesAssembler;
    private SpelAwareProxyProjectionFactory projectionFactory;

    public UserItemsController(UserTeamService userTeamService, ProjectRepository projectRepository, SubmissionRepository submissionRepository, SubmissionStatusRepository submissionStatusRepository, PagedResourcesAssembler pagedResourcesAssembler, SpelAwareProxyProjectionFactory projectionFactory) {
        this.userTeamService = userTeamService;
        this.projectRepository = projectRepository;
        this.submissionRepository = submissionRepository;
        this.submissionStatusRepository = submissionStatusRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.projectionFactory = projectionFactory;
    }

    @RequestMapping("/user/projects")
    public PagedResources<Resource<SubmittableWithStatus>> getUserProjects(Pageable pageable) {
        List<String> userTeamNames = userTeamService.userTeamNames();
        return pagedResourcesAssembler.toResource(
                projectRepository
                        .submittablesInTeams(userTeamNames, pageable)
                        .map(project -> projectionFactory.createProjection(SubmittableWithStatus.class, project))
        );
    }

    @RequestMapping("/user/submissions")
    public PagedResources<Resource<SubmissionWithStatus>> getUserSubmissions(Pageable pageable) {
        List<String> userTeamNames = userTeamService.userTeamNames();
        return pagedResourcesAssembler.toResource(
                submissionRepository
                        .findByTeamNameInOrderByCreatedByDesc(userTeamNames, pageable)
                        .map(submission -> projectionFactory.createProjection(SubmissionWithStatus.class, submission))
        );
    }

    @RequestMapping("/user/submissionStatusSummary")
    public Map<String, Integer> getUserSubmissionStatusSummary() {
        List<String> userTeamNames = userTeamService.userTeamNames();
        Map<String, Integer> statusCounts = submissionStatusRepository.submissionStatusCountsByTeam(userTeamNames);
        return statusCounts;
    }
}
