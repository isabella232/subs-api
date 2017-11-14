package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.services.UserTeamService;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@BasePathAwareController
public class ProjectsController {

    private UserTeamService userTeamService;
    private ProjectRepository projectRepository;
    private PagedResourcesAssembler<Project> pagedResourcesAssembler;

    public ProjectsController(UserTeamService userTeamService, ProjectRepository projectRepository, PagedResourcesAssembler<Project> pagedResourcesAssembler) {
        this.userTeamService = userTeamService;
        this.projectRepository = projectRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @RequestMapping("/user/projects")
    public PagedResources<Resource<Project>> getUserProjects(Pageable pageable) {
        List<Team> userTeams = userTeamService.userTeams();
        List<String> userTeamNames = userTeams.stream().map(Team::getName).collect(Collectors.toList());
        Page<Project> projects = projectRepository.submittablesInTeams(userTeamNames, pageable);

        return pagedResourcesAssembler.toResource(projects);
    }

}
