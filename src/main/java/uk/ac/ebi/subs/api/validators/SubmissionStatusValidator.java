package uk.ac.ebi.subs.api.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.data.status.StatusDescription;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionStatusRepository;

import java.util.List;
import java.util.Optional;


@Component
public class SubmissionStatusValidator implements Validator{


    @Autowired
    public SubmissionStatusValidator(
            SubmissionStatusRepository submissionStatusRepository,
            List<StatusDescription> submissionStatuses
    ){
        this.submissionStatusRepository = submissionStatusRepository;
        this.submissionStatuses = submissionStatuses;
    }


    private List<StatusDescription> submissionStatuses;
    private SubmissionStatusRepository submissionStatusRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return SubmissionStatus.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        /* unchecked */
        SubmissionStatus submissionStatus = (SubmissionStatus) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "status", "required", "status is required");

        if (errors.hasErrors()) return;

        String targetStatusName = submissionStatus.getStatus();
        Optional<StatusDescription> targetStatusDescriptionOptional = submissionStatuses.stream()
                .filter(s -> s.getStatusName().equals(targetStatusName))
                .findFirst();

        if (!targetStatusDescriptionOptional.isPresent()){
            errors.rejectValue("status","invalid status","not a recognised status");
            return;
        }

        StatusDescription targetStatusDescription = targetStatusDescriptionOptional.get();


        SubmissionStatus currentSubmissionStatus = submissionStatusRepository.findOne(submissionStatus.getId());
        Optional<StatusDescription> currentStatusDescriptionOptional = submissionStatuses.stream()
                .filter(s -> s.getStatusName().equals(currentSubmissionStatus.getStatus()))
                .findFirst();

        if (!currentStatusDescriptionOptional.isPresent()){
            //TODO how to handle this scenario - database entry has invalid status
            return;
        }

        StatusDescription currentStatusDescription = currentStatusDescriptionOptional.get();

        if (!currentStatusDescription.isUserTransitionPermitted(targetStatusName)){
            errors.rejectValue("status","invalid status change","not a permitted status change");
            return;
        }

        submissionStatus.setSubmission(currentSubmissionStatus.getSubmission());
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "submission", "required", "submission is required");

    }
}
