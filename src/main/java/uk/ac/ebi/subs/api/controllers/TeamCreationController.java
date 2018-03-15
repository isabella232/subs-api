package uk.ac.ebi.subs.api.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.aap.TeamCreationService;
import uk.ac.ebi.subs.api.aap.TeamDto;
import uk.ac.ebi.subs.api.aap.TeamNameSequenceService;
import uk.ac.ebi.subs.api.aap.UsiTokenService;
import uk.ac.ebi.subs.api.processors.TeamResourceProcessor;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.TokenService;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class TeamCreationController {






}
