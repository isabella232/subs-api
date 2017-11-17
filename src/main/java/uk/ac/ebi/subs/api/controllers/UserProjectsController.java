package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.processors.StoredSubmittableResourceProcessor;
import uk.ac.ebi.subs.api.services.IdentifiableResourceSelfLinker;
import uk.ac.ebi.subs.api.services.UserTeamService;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.projections.SubmittableWithStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@BasePathAwareController
public class UserProjectsController {

    private UserTeamService userTeamService;
    private ProjectRepository projectRepository;
    private PagedResourcesAssembler<Project> pagedResourcesAssembler;
    private IdentifiableResourceSelfLinker<Project> identifiableResourceSelfLinker;
    private StoredSubmittableResourceProcessor<Project> storedSubmittableResourceProcessor;
    private SpelAwareProxyProjectionFactory projectionFactory;

    public UserProjectsController(UserTeamService userTeamService, ProjectRepository projectRepository, PagedResourcesAssembler<Project> pagedResourcesAssembler, IdentifiableResourceSelfLinker<Project> identifiableResourceSelfLinker, StoredSubmittableResourceProcessor<Project> storedSubmittableResourceProcessor, SpelAwareProxyProjectionFactory projectionFactory) {
        this.userTeamService = userTeamService;
        this.projectRepository = projectRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.identifiableResourceSelfLinker = identifiableResourceSelfLinker;
        this.storedSubmittableResourceProcessor = storedSubmittableResourceProcessor;
        this.projectionFactory = projectionFactory;
    }

    @RequestMapping("/user/projects")
    public PagedResources<Resource<SubmittableWithStatus>> getUserProjects(Pageable pageable) {
        List<String> userTeamNames = userTeamService.userTeamNames();

        PagedResources<Resource<Project>> projectsPagedResource = pagedResourcesAssembler.toResource(
                projectRepository
                        .submittablesInTeams(userTeamNames, pageable)
        );

        PagedResources.PageMetadata pageMetadata = projectsPagedResource.getMetadata();
        Collection<Link> pageLinks = projectsPagedResource.getLinks();

        Collection<Resource<SubmittableWithStatus>> submittableWithStatus = projectsPagedResource.getContent()
                .stream()
                .map(resource -> identifiableResourceSelfLinker.addSelfLink(resource))
                .map(resource -> storedSubmittableResourceProcessor.process(resource))
                .map(resource ->
                        new Resource<>(
                                projectionFactory.createProjection(SubmittableWithStatus.class, resource.getContent()),
                                resource.getLinks()
                        )
                )
                .collect(Collectors.toList());


        return new PagedResources<>(submittableWithStatus,pageMetadata,pageLinks);
    }
}
