package uk.ac.ebi.subs.api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.validator.data.SingleValidationResult;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.data.structures.GlobalValidationStatus;
import uk.ac.ebi.subs.validator.data.structures.SingleValidationResultStatus;
import uk.ac.ebi.subs.validator.data.structures.ValidationAuthor;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by karoly on 11/07/2017.
 */
@Service
public class ValidationResultServiceImpl implements ValidationResultService {

    private ValidationResultRepository validationResultRepository;
    private SubmissionRepository submissionRepository;

    @Autowired
    public ValidationResultServiceImpl(ValidationResultRepository validationResultRepository, SubmissionRepository submissionRepository) {
        this.validationResultRepository = validationResultRepository;
        this.submissionRepository = submissionRepository;
    }

    @Override
    public List<ValidationResult> getValidationResultBySubmissionId(String submissionId) {
        return validationResultRepository.findAllBySubmissionId(submissionId);
    }

    @Override
    public boolean isValidationFinishedAndPassed(String submissionStatusId) {
        Submission submission = submissionRepository.findBySubmissionStatusId(submissionStatusId);
        List<ValidationResult> validationResults = getValidationResultBySubmissionId(submission.getId());

        return validationResults.size() > 0 && isValidationCompleted(validationResults) && isValidationPassed(validationResults);
    }

    private boolean isValidationCompleted(List<ValidationResult> validationResults) {
        return validationResults.stream().filter(
                validationResult -> validationResult.getValidationStatus() != GlobalValidationStatus.Complete)
                .findAny()
                .orElse(null) == null;
    }

    private boolean isValidationPassed(List<ValidationResult> validationResults) {
        Set<ValidationAuthor> relevantAuthorsForSample = new HashSet<>();
        relevantAuthorsForSample.add(ValidationAuthor.Core);
        relevantAuthorsForSample.add(ValidationAuthor.Biosamples);

        for (ValidationResult validationResult : validationResults) {
           Map<ValidationAuthor,String> outcomeByAuthor = validationResult.getOverallValidationOutcomeByAuthor();

            if (outcomeByAuthor.isEmpty()){
                return false;
            }

            boolean validatedDocumentIsSample = outcomeByAuthor.containsKey(ValidationAuthor.Biosamples);

            for (Map.Entry<ValidationAuthor,String> entry : outcomeByAuthor.entrySet()){
                ValidationAuthor author = entry.getKey();
                String outcome = entry.getValue();

                if (validatedDocumentIsSample && !relevantAuthorsForSample.contains(author)){
                    continue;
                }

                boolean hasError = SingleValidationResultStatus.Error.toString().equals(outcome);

                if (hasError) {
                    return false;
                }


            }
        }

        return true;
    }
}
