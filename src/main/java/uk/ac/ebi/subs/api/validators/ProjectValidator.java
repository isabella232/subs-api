package uk.ac.ebi.subs.api.validators;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;

import java.util.List;
import java.util.Optional;

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

        if (project.getSubmission() == null || project.getSubmission().getId() == null) {
            return; //it's not got a valid submission, there's no point doing the next bit
        }

        //there can only be one project in a submission

        List<Project> projectsInSubmission = projectRepository.findBySubmissionId(
                project.getSubmission().getId()
        );

        if (projectsInSubmission.isEmpty()) {
            // there's no other projects in this submission, so this one will be the first
            return;
        }

        boolean tooManyProjects = false;

        if (project.getId() == null){
            tooManyProjects = true;
        }
        else {
            Optional<Project> optionalMatchedProject = projectsInSubmission.stream()
                    .filter(storedProject -> storedProject.getId().equals(project.getId()))
                    .findAny();

            if (!optionalMatchedProject.isPresent()){
                tooManyProjects = true;
            }
        }

        if (tooManyProjects){
            SubsApiErrors.already_exists.addError(errors);
        }

    }
}
