package uk.ac.ebi.subs.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.validators.*;

import java.util.stream.Stream;


@Configuration
/**
 * Frontend validator configuration.
 * Using manual linking of validators as the automatic discovery described
 * in the docs @see <a href="http://docs.spring.io/spring-data/rest/docs/current/reference/html/#validation">docs.spring.io</a>
 * does not currently work
 *
 * Fixing this is in Spring JIRA @see <a href="https://jira.spring.io/browse/DATAREST-524">DATAREST-524</a>
 */
public class ValidatorConfig extends RepositoryRestConfigurerAdapter {

    private static final String BEFORE_CREATE = "beforeCreate";
    private static final String BEFORE_SAVE = "beforeSave";
    private static final String BEFORE_LINK_SAVE = "beforeLinkSave";
    private static final String BEFORE_DELETE = "beforeDelete";

    public ValidatorConfig(AnalysisValidator analysisValidator, AssayValidator assayValidator, AssayDataValidator assayDataValidator, EgaDacValidator egaDacValidator, EgaDacPolicyValidator egaDacPolicyValidator, EgaDatasetValidator egaDatasetValidator, ProjectValidator projectValidator, ProtocolValidator protocolValidator, SampleValidator sampleValidator, SampleGroupValidator sampleGroupValidator, StudyValidator studyValidator, SubmissionValidator submissionValidator, SubmissionDeleteValidator submissionDeleteValidator, SubmittableDeleteValidator submittableDeleteValidator, SubmissionStatusValidator submissionStatusValidator) {
        this.analysisValidator = analysisValidator;
        this.assayValidator = assayValidator;
        this.assayDataValidator = assayDataValidator;
        this.egaDacValidator = egaDacValidator;
        this.egaDacPolicyValidator = egaDacPolicyValidator;
        this.egaDatasetValidator = egaDatasetValidator;
        this.projectValidator = projectValidator;
        this.protocolValidator = protocolValidator;
        this.sampleValidator = sampleValidator;
        this.sampleGroupValidator = sampleGroupValidator;
        this.studyValidator = studyValidator;
        this.submissionValidator = submissionValidator;
        this.submissionDeleteValidator = submissionDeleteValidator;
        this.submittableDeleteValidator = submittableDeleteValidator;
        this.submissionStatusValidator = submissionStatusValidator;
    }

    private AnalysisValidator analysisValidator;
    private AssayValidator assayValidator;
    private AssayDataValidator assayDataValidator;
    private EgaDacValidator egaDacValidator;
    private EgaDacPolicyValidator egaDacPolicyValidator;
    private EgaDatasetValidator egaDatasetValidator;
    private ProjectValidator projectValidator;
    private ProtocolValidator protocolValidator;
    private SampleValidator sampleValidator;
    private SampleGroupValidator sampleGroupValidator;
    private StudyValidator studyValidator;
    private SubmissionValidator submissionValidator;
    private SubmissionDeleteValidator submissionDeleteValidator;
    private SubmittableDeleteValidator submittableDeleteValidator;
    private SubmissionStatusValidator submissionStatusValidator;


    @Override
    public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener eventListener) {

        Stream<Validator> stdValidators = Stream.of(
                analysisValidator,
                assayValidator,
                assayDataValidator,
                egaDacValidator,
                egaDacPolicyValidator,
                egaDatasetValidator,
                projectValidator,
                protocolValidator,
                sampleValidator,
                sampleGroupValidator,
                studyValidator,

                submissionValidator

        );

        stdValidators.forEach(validator -> {
            eventListener.addValidator(BEFORE_CREATE, validator);
            eventListener.addValidator(BEFORE_SAVE, validator);
        });

        eventListener.addValidator(BEFORE_SAVE, submissionStatusValidator);

        eventListener.addValidator(BEFORE_DELETE, submissionDeleteValidator);

        eventListener.addValidator(BEFORE_DELETE, submittableDeleteValidator);
    }

}
