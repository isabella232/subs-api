package uk.ac.ebi.subs.api.converters;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.controllers.SubmissionDTO;
import uk.ac.ebi.subs.api.services.UriToEntityConversionService;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionPlan;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Helper class to instantiate a new {@link Submission} entity
 * and set some of its properties based on the the {@link SubmissionDTO} value object's properties.
 */
@RequiredArgsConstructor
@Component
public class SubmissionDTOConverter implements Converter<SubmissionDTO, Submission> {

    @NonNull
    private UriToEntityConversionService conversionService;

    @Override
    public Submission convert(SubmissionDTO submissionDTO) {
        Submission submission = new Submission();
        submission.setName(submissionDTO.getName());
        submission.setProjectName(submissionDTO.getProjectName());
        submission.setUiData(submissionDTO.getUiData());

        String submissionPlanURIString = submissionDTO.getSubmissionPlan();

        if (submissionPlanURIString != null) {
            SubmissionPlan submissionPlan = null;
            try {
                submissionPlan = conversionService.convert(new URI(submissionPlanURIString), SubmissionPlan.class);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(String.format("Can not convert the given string: %s to a URI", submissionPlanURIString) , e);
            }
            submission.setSubmissionPlan(submissionPlan);
        }

        return submission;

    }
}
