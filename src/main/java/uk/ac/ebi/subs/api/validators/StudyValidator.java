package uk.ac.ebi.subs.api.validators;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.Study;
import uk.ac.ebi.subs.repository.repos.submittables.StudyRepository;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class StudyValidator implements Validator {


    public StudyValidator(CoreSubmittableValidationHelper coreSubmittableValidationHelper, StudyRepository studyRepository) {
        this.coreSubmittableValidationHelper = coreSubmittableValidationHelper;
        this.studyRepository = studyRepository;
    }

    private CoreSubmittableValidationHelper coreSubmittableValidationHelper;
    private StudyRepository studyRepository;


    @Override
    public boolean supports(Class<?> clazz) {
        return Study.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Study study = (Study) target;
        coreSubmittableValidationHelper.validate(study, studyRepository, errors);
        validateAliasIsLockedToDataType(study, errors);

    }

    public void validateAliasIsLockedToDataType(Study target, Errors errors) {
        if (target.getAlias() == null) {
            return;
        }

        Stream<Study> itemsWithAliasStream = studyRepository.streamByTeamNameAndAliasOrderByCreatedDateDesc(
                target.getSubmission().getTeam().getName(),
                target.getAlias());

        Set<String> dataTypeIds = itemsWithAliasStream
                .map(study -> study.getDataType())
                .filter(Objects::nonNull)
                .map(dataType -> dataType.getId())
                .distinct()
                .collect(Collectors.toSet());


        if (dataTypeIds.size() > 1) {
            throw new IllegalStateException("Multiple dataTypes found in history of item " + target);
        }

        if (dataTypeIds.size() == 1 && !target.getDataType().getId().equals(dataTypeIds.iterator().next())) {
            SubsApiErrors.invalid.addError(errors, "dataType");
        }
    }
}

