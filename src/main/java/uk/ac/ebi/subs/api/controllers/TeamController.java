package uk.ac.ebi.subs.api.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.aap.TeamCreationService;
import uk.ac.ebi.subs.api.aap.TeamDto;
import uk.ac.ebi.subs.api.processors.TeamResourceProcessor;
import uk.ac.ebi.subs.api.services.UserTeamService;
import uk.ac.ebi.subs.api.services.UserTokenService;
import uk.ac.ebi.subs.api.validators.TeamDtoValidator;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.security.PreAuthorizeParamTeamName;
import uk.ac.ebi.tsc.aap.client.model.User;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * It contains endpoints related to {@link Team} entity.
 */
@RestController
@BasePathAwareController
@RequiredArgsConstructor
public class TeamController {

    @NonNull
    private PagedResourcesAssembler<Team> teamPagedResourcesAssembler;

    @NonNull
    private UserTeamService userTeamService;

    @NonNull
    private TeamCreationService teamCreationService;

    @NonNull
    private TeamResourceProcessor teamResourceProcessor;

    @NonNull
    private TeamDtoValidator teamDtoValidator;

    @NonNull
    private UserTokenService userTokenService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Retrieve a pageable list of {@link Team} entities.
     * @param authorizationHeader the authorization header in string required by AAP.
     * @param pageable pagination information
     * @return the list of the teams belongs to the given user
     */
    @RequestMapping("/user/teams")
    public CollectionModel<EntityModel<Team>> getTeams(@RequestHeader("Authorization") String authorizationHeader,
                                              @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {

        String token = userTokenService.authorizationHeaderValueToToken(authorizationHeader);
        List<Team> teamList = userTeamService.userTeams(token);

        // this is a workaround to make paging to work, because TSC/AAP not providing us a pageable Team collection
        // otherwise it would be: final PageImpl<Team> teams = new PageImpl<>(teamList, pageable, teamList.size());
        int start = Long.valueOf(pageable.getOffset()).intValue();
        final int initialEndValue = start + pageable.getPageSize();
        int end = initialEndValue > teamList.size() ? teamList.size() : initialEndValue;
        final Page<Team> teams = new PageImpl<>(teamList.subList(start, end), pageable, teamList.size());

        return teamPagedResourcesAssembler.toModel(teams);
    }

    /**
     * Retrieve information of a given team.
     * @param teamName the name of the team to get information of
     * @return the requested {@link Team} resource
     */
    @RequestMapping("/teams/{teamName:.+}")
    @PreAuthorizeParamTeamName
    public EntityModel<Team> getTeam(@PathVariable @P("teamName") String teamName) {

        Team d = new Team();
        d.setName(teamName);

        EntityModel<Team> resource = new EntityModel<>(d);

        addSelfLink(d, resource);

        return resource;
    }

    private void addSelfLink(Team d, EntityModel<Team> resource) {
        resource.add(
                linkTo(
                        methodOn(this.getClass()).getTeam(
                                d.getName()
                        )
                ).withSelfRel()
        );
    }

    /**
     * Creates a new team for the specified user.
     *
     * @param teamDto the request payload containing the data of the team to be created
     * @param result binding result for the validator to populate with the list of errors
     * @return the created {@link Team} resource
     */
    @RequestMapping(value = "/user/teams", method = RequestMethod.POST)
    public ResponseEntity<EntityModel<Team>> createTeam(@RequestBody TeamDto teamDto, BindingResult result) {

        teamDtoValidator.validate(teamDto, result);

        if (result.hasErrors()) {
            throw new RepositoryConstraintViolationException(result);
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getDetails();

        Team team = teamCreationService.createTeam(user, teamDto);

        EntityModel<Team> teamResource = new EntityModel<>(team);

        addSelfLink(team, teamResource);

        return new ResponseEntity<>(
                teamResourceProcessor.process(teamResource),
                HttpStatus.CREATED
        );
    }
}
