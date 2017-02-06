package uk.ac.ebi.subs.api.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.validation.Validator;

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


    @Autowired
    AnalysisValidator analysisValidator;

    @Autowired
    private AssayValidator assayValidator;

    @Autowired
    private AssayDataValidator assayDataValidator;

    @Autowired
    private EgaDacValidator egaDacValidator;

    @Autowired
    private EgaDacPolicyValidator egaDacPolicyValidator;

    @Autowired
    private EgaDatasetValidator egaDatasetValidator;

    @Autowired
    private ProjectValidator projectValidator;

    @Autowired
    private ProtocolValidator protocolValidator;

    @Autowired
    private SampleValidator sampleValidator;

    @Autowired
    private SampleGroupValidator sampleGroupValidator;

    @Autowired
    private StudyValidator studyValidator;

    @Autowired
    private SubmissionValidator submissionValidator;

    @Autowired
    private SubmissionDeleteValidator submissionDeleteValidator;

    @Autowired
    private SubmittableDeleteValidator submittableDeleteValidator;


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

        eventListener.addValidator(BEFORE_DELETE,submissionDeleteValidator);

        eventListener.addValidator(BEFORE_DELETE,submittableDeleteValidator);
    }

}
