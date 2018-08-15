package uk.ac.ebi.subs.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.api.services.SubmittableValidationDispatcher;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.Study;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;
import uk.ac.ebi.subs.repository.repos.submittables.StudyRepository;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileRepositoryRest;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@ActiveProfiles({"SubmittableValidationDispatcherTest","basic_auth"})
@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SubmittableHandlerTest {
    @LocalServerPort
    private int port;
    private String rootUri;

    private ApiIntegrationTestHelper testHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private SampleRepository sampleRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;
    @Autowired
    private ValidationResultRepository validationResultRepository;

    @Autowired
    private SubmittableValidationDispatcher submittableValidationDispatcher;

    @MockBean private RabbitMessagingTemplate rabbitMessagingTemplate;
    @MockBean private DomainService domainService;
    @MockBean private ProfileRepositoryRest profileRepositoryRest;

    @Before
    public void buildUp() {
        clearDb();

        rootUri = "http://localhost:" + port + "/api";
        final Map<String, String> standardGetContentHeader = ApiIntegrationTestHelper.createStandardGetHeader();
        standardGetContentHeader.putAll(ApiIntegrationTestHelper.createBasicAuthheaders(TestWebSecurityConfig.USI_USER,TestWebSecurityConfig.USI_PASSWORD));
        final Map<String, String> standardPostContentHeader = ApiIntegrationTestHelper.createStandardGetHeader();
        standardPostContentHeader.putAll(ApiIntegrationTestHelper.createBasicAuthheaders(TestWebSecurityConfig.USI_USER,TestWebSecurityConfig.USI_PASSWORD));
        testHelper = new ApiIntegrationTestHelper(objectMapper, rootUri,
                Arrays.asList(submissionRepository, sampleRepository, submissionStatusRepository),standardGetContentHeader,standardPostContentHeader);

        submittableValidationDispatcher.setRabbitMessagingTemplate(rabbitMessagingTemplate);

        ApiIntegrationTestHelper.mockAapProfileAndDomain(domainService,profileRepositoryRest);
    }

    @Test
    public void testValidationMessageSamplesOnSubmit() throws Exception {
        Submission submission = Helpers.generateSubmission();
        testHelper.submissionWithSamples(submission, testHelper.rootRels());
        verify(submittableValidationDispatcher, atLeast(1)).validateCreate(any(Sample.class));
    }

    @Test
    public void testValidationMessageStudiesOnSubmit() throws Exception {
        testHelper.submissionWithStudies(testHelper.rootRels());

        verify(submittableValidationDispatcher, atLeast(1)).validateCreate(any(Study.class));
    }

    @After
    public void tearDown() {
        clearDb();
    }

    private void clearDb() {
        submissionRepository.deleteAll();
        sampleRepository.deleteAll();
        studyRepository.deleteAll();
        submissionStatusRepository.deleteAll();
        validationResultRepository.deleteAll();
    }
}
