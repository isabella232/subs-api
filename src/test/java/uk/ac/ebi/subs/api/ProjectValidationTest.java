package uk.ac.ebi.subs.api;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.api.validators.ProjectValidator;
import uk.ac.ebi.subs.data.component.Submitter;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;
import uk.ac.ebi.subs.repository.services.SubmissionHelperService;
import uk.ac.ebi.subs.repository.services.SubmittableHelperService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@WithMockUser(username = "usi_user", roles = {CoreValidatorTest.TEST_TEAM_1,Helpers.ADMIN_TEAM_NAME})
public class ProjectValidationTest {

    @Autowired
    private ProjectValidator projectValidator;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private SubmissionHelperService submissionHelperService;
    @Autowired
    private SubmittableHelperService submittableHelperService;
    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;
    @Autowired
    private ProcessingStatusRepository processingStatusRepository;

    private Team team = Team.build(CoreValidatorTest.TEST_TEAM_1);
    private Submitter submitter = Submitter.build("test@test.ac.uk");
    private Submission sub;
    private Project p;

    @Before
    public void buildUp() {
        tearDown();

        sub = submissionHelperService.createSubmission(team,submitter);

        p = new Project();
        p.setAlias("victor");
        p.setSubmission(sub);
        submittableHelperService.setupNewSubmittable(p);

        projectRepository.save(p);
    }

    @After
    public void tearDown() {
        projectRepository.deleteAll();
        submissionRepository.deleteAll();
        submissionStatusRepository.deleteAll();
        processingStatusRepository.deleteAll();
    }

    @Test
    public void projectForSubDifferentId() {
        Project freshProject = new Project();
        freshProject.setAlias("bob");
        freshProject.setSubmission(sub);
        Errors errors = new BeanPropertyBindingResult(freshProject, "project");
        projectValidator.validate(freshProject, errors);

        Assert.assertEquals(1, errors.getErrorCount());
    }

    @Test
    public void projectForSubSameId() {
        Project freshProject = new Project();
        freshProject.setAlias("victor");
        freshProject.setSubmission(sub);
        freshProject.setId(p.getId());
        Errors errors = new BeanPropertyBindingResult(freshProject, "project");
        projectValidator.validate(freshProject, errors);

        Assert.assertEquals(0, errors.getErrorCount());
    }

    @Test
    public void firstProjectForSub(){
        projectRepository.deleteAll();

        Project freshProject = new Project();
        freshProject.setAlias("victor");
        freshProject.setSubmission(sub);
        freshProject.setId(p.getId());
        Errors errors = new BeanPropertyBindingResult(freshProject, "project");
        projectValidator.validate(freshProject, errors);

        Assert.assertEquals(0, errors.getErrorCount());

    }

}
