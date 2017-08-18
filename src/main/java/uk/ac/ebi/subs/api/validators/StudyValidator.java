package uk.ac.ebi.subs.api.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.component.StudyDataType;
import uk.ac.ebi.subs.data.submittable.Submittable;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Study;
import uk.ac.ebi.subs.repository.repos.submittables.StudyRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;

import java.util.List;
import java.util.Objects;
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
        validateStudyTypeIsLockedToAlias(study,errors);

    }

    public void validateStudyTypeIsLockedToAlias(Study target, Errors errors) {
        if (target.getAlias() == null ){
            return;
        }

        List<StudyDataType> studyDataTypes;

        try (Stream<Study> itemsWithAliasStream = studyRepository.streamByTeamNameAndAliasOrderByCreatedDateDesc(
                target.getSubmission().getTeam().getName(),
                target.getAlias()
        )){
            studyDataTypes = itemsWithAliasStream
                    .map(study -> study.getStudyType())
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }

        if (studyDataTypes.size() > 1){
            throw new IllegalStateException("Multiple archives found in history of item "+target);
        }

        if (studyDataTypes.size() == 1 && !target.getStudyType().equals( studyDataTypes.get(0) )){
            SubsApiErrors.invalid.addError(errors,"studyType");
        }
    }
}

