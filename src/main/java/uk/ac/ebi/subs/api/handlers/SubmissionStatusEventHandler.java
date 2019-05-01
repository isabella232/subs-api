package uk.ac.ebi.subs.api.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.ac.ebi.subs.api.services.SubmissionEventService;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * This class is responsible handling events related to {@link SubmissionStatus} deletion.
 * Created by davidr on 20/02/2017.
 */
@Component
@RepositoryEventHandler
public class SubmissionStatusEventHandler {

    private static final String TOKEN_HEADER_KEY = "Authorization";
    private static final String TOKEN_HEADER_VALUE_PREFIX = "Bearer";

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionStatusEventHandler.class);

    private SubmissionRepository submissionRepository;
    private SubmissionEventService submissionEventService;
    private HttpServletRequest httpServletRequest;

    public SubmissionStatusEventHandler(SubmissionRepository submissionRepository,
                                        SubmissionEventService submissionEventService,
                                        HttpServletRequest httpServletRequest) {
        this.submissionRepository = submissionRepository;
        this.submissionEventService = submissionEventService;
        this.httpServletRequest = httpServletRequest;
    }

    public void setSubmissionRepository(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public void setSubmissionEventService(SubmissionEventService submissionEventService) {
        this.submissionEventService = submissionEventService;
    }

    @HandleAfterSave
    public void handleUpdate(SubmissionStatus submissionStatus) {
        if (SubmissionStatusEnum.Submitted.name().equals(submissionStatus.getStatus())) {
            Submission submission = submissionRepository.findBySubmissionStatusId(submissionStatus.getId());

            if (submission != null) {
                submissionEventService.submissionSubmitted(createSubmissionEnvelope(submission));

                submission.setSubmissionDate(new Date());
                submissionRepository.save(submission);
            }
        }
    }

    private SubmissionEnvelope createSubmissionEnvelope(Submission submission) {
        String jwtToken = extractToken();

        SubmissionEnvelope submissionEnvelope = new SubmissionEnvelope(submission);
        submissionEnvelope.setJWTToken(jwtToken);
        return submissionEnvelope;
    }

    private String extractToken() {
        final String authHeader = httpServletRequest.getHeader(TOKEN_HEADER_KEY);
        final boolean isBearerToken = authHeader.trim().startsWith(TOKEN_HEADER_VALUE_PREFIX.trim());
        if (authHeader == null || authHeader.isEmpty()) {
            LOGGER.warn("No {} authHeader", TOKEN_HEADER_KEY);
            return null;
        } else if (!isBearerToken) {
            LOGGER.warn("No {} prefix", TOKEN_HEADER_VALUE_PREFIX);
            return null;
        }
        final String jwtToken = authHeader.substring(TOKEN_HEADER_VALUE_PREFIX.trim().length());
        if (StringUtils.isEmpty(jwtToken)) {
            LOGGER.warn("Missing JWT token");
            return null;
        }

        return jwtToken;
    }
}
