package uk.ac.ebi.subs.api.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.status.SubmissionStatus;

@Component
public class SubmissionDeleteValidator implements Validator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean supports(Class<?> clazz) {
        return Submission.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        Submission submission = (Submission) target;

        if (!SubmissionStatus.Draft.name().equals(submission.getStatus())) {
            errors.reject("cannotDeleteAfterSubmission", "Deletion is not possible after submission");
        }


    }


}
