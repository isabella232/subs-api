package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.processors.SubmissionResourceProcessor;
import uk.ac.ebi.subs.api.services.UserTeamService;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.projections.SubmissionWithStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;

import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@BasePathAwareController
public class UserSubmissionsController {

    private UserTeamService userTeamService;
    private SubmissionRepository submissionRepository;
    private SubmissionStatusRepository submissionStatusRepository;
    private SubmissionResourceProcessor submissionResourceProcessor;
    private IdentifiablePageToProjectionPage<Submission,SubmissionWithStatus> identifiablePageToProjectionPage;

    public UserSubmissionsController(UserTeamService userTeamService, SubmissionRepository submissionRepository, SubmissionStatusRepository submissionStatusRepository, SubmissionResourceProcessor submissionResourceProcessor, IdentifiablePageToProjectionPage<Submission, SubmissionWithStatus> identifiablePageToProjectionPage) {
        this.userTeamService = userTeamService;
        this.submissionRepository = submissionRepository;
        this.submissionStatusRepository = submissionStatusRepository;
        this.submissionResourceProcessor = submissionResourceProcessor;
        this.identifiablePageToProjectionPage = identifiablePageToProjectionPage;
    }

    @RequestMapping("/user/submissions")
    public PagedResources<Resource<SubmissionWithStatus>> getUserSubmissions(Pageable pageable) {
        List<String> userTeamNames = userTeamService.userTeamNames();

        Page<Submission> page = submissionRepository.findByTeamNameInOrderByCreatedByDesc(userTeamNames, pageable);

        return identifiablePageToProjectionPage.convert(
                page,
                pageable,
                submissionResourceProcessor,
                SubmissionWithStatus.class
        );
    }

    @RequestMapping("/user/submissionStatusSummary")
    public Resource<Map<String, Integer>> getUserSubmissionStatusSummary() {
        List<String> userTeamNames = userTeamService.userTeamNames();
        Map<String, Integer> statusCounts = submissionStatusRepository.submissionStatusCountsByTeam(userTeamNames);

        Link self = linkTo(methodOn(this.getClass()).getUserSubmissionStatusSummary()).withSelfRel();

        return new Resource<>(statusCounts,self);
    }
}
