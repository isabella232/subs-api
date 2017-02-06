package uk.ac.ebi.subs.api.validators;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.repos.ProjectRepository;

@Component
public class ProjectValidator implements Validator {

    @Autowired
    private CoreSubmittableValidationHelper coreSubmittableValidationHelper;
    @Autowired
    private ProjectRepository projectRepository;


    @Override
    public boolean supports(Class<?> clazz) {
        return Project.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Project project = (Project) target;
        coreSubmittableValidationHelper.validate(project, projectRepository, errors);
    }
}
