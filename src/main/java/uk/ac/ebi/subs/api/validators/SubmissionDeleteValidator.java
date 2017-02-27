package uk.ac.ebi.subs.api.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.services.OperationControlService;

import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.model.Submission;

@Component
public class SubmissionDeleteValidator implements Validator {

    @Autowired
    public SubmissionDeleteValidator(SubmissionRepository submissionRepository, OperationControlService operationControlService) {
        this.submissionRepository = submissionRepository;
        this.operationControlService = operationControlService;
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private SubmissionRepository submissionRepository;
    private OperationControlService operationControlService;

    @Override
    public boolean supports(Class<?> clazz) {
        return Submission.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        Submission submission = (Submission) target;

        Submission storedSub = submissionRepository.findOne(submission.getId());


        if (!SubmissionStatusEnum.Draft.name().equals(storedSub.getSubmissionStatus().getStatus())) {
            SubsApiErrors.resource_locked.addError(errors);
        }


    }


}
