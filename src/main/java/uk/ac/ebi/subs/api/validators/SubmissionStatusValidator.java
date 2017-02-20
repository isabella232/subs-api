package uk.ac.ebi.subs.api.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.data.status.StatusDescription;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionStatusRepository;

import java.util.Map;


@Component
public class SubmissionStatusValidator implements Validator {


    @Autowired
    public SubmissionStatusValidator(
            SubmissionStatusRepository submissionStatusRepository,
            OperationControlService operationControlService,
            Map<String, StatusDescription> submissionStatusDescriptionMap

    ) {
        this.submissionStatusRepository = submissionStatusRepository;
        this.submissionStatusDescriptionMap = submissionStatusDescriptionMap;
    }


    private Map<String, StatusDescription> submissionStatusDescriptionMap;
    private SubmissionStatusRepository submissionStatusRepository;
    private OperationControlService operationControlService;

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


        if (!submissionStatusDescriptionMap.containsKey(targetStatusName)) {
            errors.rejectValue("status", "invalid status", "not a recognised status");
            return;
        }

        StatusDescription targetStatusDescription = submissionStatusDescriptionMap.get(targetStatusName);


        SubmissionStatus currentSubmissionStatus = submissionStatusRepository.findOne(submissionStatus.getId());
        StatusDescription currentStatusDescription = submissionStatusDescriptionMap.get(currentSubmissionStatus.getStatus());

        if (!currentStatusDescription.isUserTransitionPermitted(targetStatusName)) {
            errors.rejectValue("status", "invalid status change", "not a permitted status change");
            return;
        }
    }
}
