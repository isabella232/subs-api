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

        if (!submissionStatusService.isSubmissionStatusChangeable(submissionStatus)){
            SubsApiErrors.resource_locked.addError(errors,"status");
            return;
        }

        Submission submission = submissionRepository.findBySubmissionStatusId(submissionStatus.getId());
        if (!submissionStatusService.getAvailableStatusNames(submission, submissionStatusDescriptionMap)
                .contains(targetStatusName)) {
            SubsApiErrors.invalid.addError(errors,"status");
            return;
        }

    }
}
