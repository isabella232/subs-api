package uk.ac.ebi.subs.api.validators;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.services.SubmissionStatusService;
import uk.ac.ebi.subs.api.services.ValidationResultService;
import uk.ac.ebi.subs.data.status.StatusDescription;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubmissionStatusValidator implements Validator {

    @NonNull
    private Map<String, StatusDescription> submissionStatusDescriptionMap;
    @NonNull
    private SubmissionStatusRepository submissionStatusRepository;
    @NonNull
    private ValidationResultService validationResultService;
    @NonNull
    private SubmissionRepository submissionRepository;
    @NonNull
    private SubmissionStatusService submissionStatusService;

    @Override
    public boolean supports(Class<?> clazz) {
        return SubmissionStatus.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        /* unchecked */
        SubmissionStatus submissionStatus = (SubmissionStatus) target;

        SubsApiErrors.rejectIfEmptyOrWhitespace(errors,"status");

        if (errors.hasErrors()) return;

        String targetStatusName = submissionStatus.getStatus();

        if (!submissionStatusDescriptionMap.containsKey(targetStatusName)) {
            SubsApiErrors.invalid.addError(errors,"status");
            return;
        }

        Submission submission = submissionRepository.findBySubmissionStatusId(submissionStatus.getId());
        if (!submissionStatusService.getAvailableStatusNames(submission, submissionStatusDescriptionMap)
                .contains(SubmissionStatusEnum.Submitted.name())) {
            SubsApiErrors.invalid.addError(errors,"status");
            return;
        }

        SubmissionStatus currentSubmissionStatus = submissionStatusRepository.findOne(submissionStatus.getId());
        StatusDescription currentStatusDescription = submissionStatusDescriptionMap.get(currentSubmissionStatus.getStatus());

        submissionStatus.setCreatedDate(currentSubmissionStatus.getCreatedDate());
        submissionStatus.setCreatedBy(currentSubmissionStatus.getCreatedBy());

        if (!currentStatusDescription.isUserTransitionPermitted(targetStatusName)) {
            SubsApiErrors.invalid.addError(errors,"status");
            return;
        }

        if (!validationResultService.isValidationFinishedAndPassed(submissionStatus.getId())) {
            SubsApiErrors.invalid.addError(errors,"status");
            return;
        }
    }
}
