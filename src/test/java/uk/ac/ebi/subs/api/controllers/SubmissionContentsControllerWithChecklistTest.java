package uk.ac.ebi.subs.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.RabbitMQDependentTest;
import uk.ac.ebi.subs.api.ApiIntegrationTestHelper;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.api.utils.SubmittableHelper;
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.component.SampleExternalReference;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileRepositoryRest;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.subs.api.utils.SubmittableHelper.createSample;
import static uk.ac.ebi.subs.api.utils.SubmittableHelper.createSubmissionStatus;
import static uk.ac.ebi.subs.api.utils.SubmittableHelper.generateChecklist;
import static uk.ac.ebi.subs.api.utils.SubmittableHelper.generateDataType;
import static uk.ac.ebi.subs.api.utils.ValidationResultHelper.SAMPLES_DATA_TYPE_ID;
import static uk.ac.ebi.subs.api.utils.ValidationResultHelper.SUBMISSION_ID;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@Category(RabbitMQDependentTest.class)
@WithMockUser(username = "usi_admin_user", roles = {Helpers.TEAM_NAME})
public class SubmissionContentsControllerWithChecklistTest {

    private static final String CHECKLIST_ERC_000021 = "ERC000021";
    private static final String CHECKLIST_NOT_EXISTS = "ERC_NOT_EXISTS";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DataTypeRepository dataTypeRepository;
    @Autowired
    private ChecklistRepository checklistRepository;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;

    @MockBean
    private DomainService domainService;
    @MockBean
    private ProfileRepositoryRest profileRepositoryRest;

    @Before
    public void setup() {
        cleanRepositories();

        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();

        DataType samplesDataType = generateDataType(Archive.BioSamples, "samples", Sample.class.getName());
        dataTypeRepository.save(samplesDataType);

        Checklist checklist = generateChecklist(CHECKLIST_ERC_000021, samplesDataType.getId());
        checklistRepository.save(checklist);

        final Submission submission = SubmittableHelper.createSubmission(SUBMISSION_ID);
        submissionRepository.save(submission);

        final SubmissionStatus submissionStatus = createSubmissionStatus();
        submissionStatusRepository.save(submissionStatus);

        submission.setSubmissionStatus(submissionStatus);
        submissionRepository.save(submission);

        ApiIntegrationTestHelper.mockAapProfileAndDomain(domainService, profileRepositoryRest);
    }

    @After
    public void tearDown() {
        cleanRepositories();
    }

    private void cleanRepositories() {
        dataTypeRepository.deleteAll();
        checklistRepository.deleteAll();
        submissionStatusRepository.deleteAll();
        submissionRepository.deleteAll();
    }

    @Test
    public void whenCreatingDataTypeWithoutChecklist_ThenChecklistShouldBeNull() throws Exception{
        Sample sample = createSample(false, null);
        String json = objectMapper.writeValueAsString(sample);

        mockMvc.perform(post(String.format("/submissions/%s/contents/%s", SUBMISSION_ID, SAMPLES_DATA_TYPE_ID))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$._embedded.checklist").doesNotExist());
    }

    @Test
    public void whenCreatingDataTypeWithNonExistingChecklistID_ThenReturnsCheckListNotFoundResult() throws Exception {
        Sample sample = createSample(false, null);

        ObjectNode jsonObject = objectMapper.readValue(this.objectMapper.writeValueAsString(sample), ObjectNode.class);
        jsonObject.put("checklistId", CHECKLIST_NOT_EXISTS);
        String json = objectMapper.writeValueAsString(jsonObject);

        mockMvc.perform(post(String.format("/submissions/%s/contents/%s", SUBMISSION_ID, SAMPLES_DATA_TYPE_ID))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString(CHECKLIST_NOT_EXISTS)));
    }

    @Test
    public void whenCreatingDataTypeWithChecklistID_ThenChecklistShouldBePopulatedInSubmittable() throws Exception {
        Sample sample = createSample(false, null);

        ObjectNode jsonObject = objectMapper.readValue(this.objectMapper.writeValueAsString(sample), ObjectNode.class);
        jsonObject.put("checklistId", CHECKLIST_ERC_000021);
        String json = objectMapper.writeValueAsString(jsonObject);

        mockMvc.perform(post(String.format("/submissions/%s/contents/%s", SUBMISSION_ID, SAMPLES_DATA_TYPE_ID))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$._embedded.checklist").exists())
                .andExpect(jsonPath("$._embedded.checklist.id", is(equalTo(CHECKLIST_ERC_000021))));
    }

    @Test
    public void whenCreatingSampleWithExternalRelationShip_ThenExternalRelationshipShouldBePersisted() throws Exception {
        Sample sample = createSample(false, null);

        List<SampleExternalReference> externalReferences = new ArrayList<>();

        SampleExternalReference externalReference = new SampleExternalReference();
        final String exampleSampleRefUrl = "http://sampleexample.com/ref1";
        externalReference.setUrl(exampleSampleRefUrl);

        externalReferences.add(externalReference);

        sample.setSampleExternalReferences(externalReferences);

        String json = objectMapper.writeValueAsString(sample);

        mockMvc.perform(post(String.format("/submissions/%s/contents/%s", SUBMISSION_ID, SAMPLES_DATA_TYPE_ID))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
                .andDo(print())
                .andExpect(status().isCreated());
    }
}
