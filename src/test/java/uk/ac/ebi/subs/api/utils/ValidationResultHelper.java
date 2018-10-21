package uk.ac.ebi.subs.api.utils;

import uk.ac.ebi.subs.validator.data.SingleValidationResult;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.data.structures.GlobalValidationStatus;
import uk.ac.ebi.subs.validator.data.structures.SingleValidationResultStatus;
import uk.ac.ebi.subs.validator.data.structures.ValidationAuthor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResultHelper {

    public static final String SUBMISSION_ID = "111-222-3333";
    public static final String SAMPLES_DATA_TYPE_ID = "samples";


    public static Map<ValidationAuthor, List<SingleValidationResult>> generateExpectedResults(
            List<SingleValidationResultStatus> singleValidationResultStatuses) {
        Map<ValidationAuthor, List<SingleValidationResult>> expectedResult = new HashMap<>();
        expectedResult.put(ValidationAuthor.Core,
                Collections.singletonList(generateSingleValidationResult(singleValidationResultStatuses.get(0))));
        expectedResult.put(ValidationAuthor.Ena,
                Collections.singletonList(generateSingleValidationResult(singleValidationResultStatuses.get(1))));
        expectedResult.put(ValidationAuthor.JsonSchema,
                Collections.singletonList(generateSingleValidationResult(singleValidationResultStatuses.get(2))));

        return expectedResult;
    }

    public static SingleValidationResult generateSingleValidationResult(SingleValidationResultStatus singleValidationResultStatus) {
        SingleValidationResult singleValidationResult = new SingleValidationResult();
        singleValidationResult.setValidationAuthor(ValidationAuthor.Biosamples);
        singleValidationResult.setValidationStatus(singleValidationResultStatus);

        return singleValidationResult;
    }

    public static ValidationResult generateValidationResult(Map<ValidationAuthor, List<SingleValidationResult>> validationResultByValidationAuthors) {
        ValidationResult validationResult = new ValidationResult();
        validationResult.setDataTypeId(SAMPLES_DATA_TYPE_ID);
        validationResult.setSubmissionId(SUBMISSION_ID);
        validationResult.setExpectedResults(validationResultByValidationAuthors);
        validationResult.setValidationStatus(GlobalValidationStatus.Complete);

        return validationResult;
    }

}
