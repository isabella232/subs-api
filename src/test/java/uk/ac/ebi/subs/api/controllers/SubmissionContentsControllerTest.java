package uk.ac.ebi.subs.api.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.api.ApiIntegrationTestHelper;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.api.processors.LinkHelper;
import uk.ac.ebi.subs.api.processors.StoredSubmittableAssembler;
import uk.ac.ebi.subs.api.processors.StoredSubmittableResourceProcessor;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;
import uk.ac.ebi.subs.validator.data.SingleValidationResult;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.data.structures.SingleValidationResultStatus;
import uk.ac.ebi.subs.validator.data.structures.ValidationAuthor;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileRepositoryRest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.subs.api.utils.ValidationResultHelper.SAMPLES_DATA_TYPE_ID;
import static uk.ac.ebi.subs.api.utils.ValidationResultHelper.SUBMISSION_ID;
import static uk.ac.ebi.subs.api.utils.ValidationResultHelper.generateExpectedResults;
import static uk.ac.ebi.subs.api.utils.ValidationResultHelper.generateValidationResult;

@RunWith(SpringRunner.class)
@WebMvcTest(SubmissionContentsController.class)
@MockBeans({
        @MockBean(DataTypeRepository.class),
        @MockBean(RepositoryEntityLinks.class),
        @MockBean(StoredSubmittableAssembler.class),
        @MockBean(StoredSubmittableResourceProcessor.class)
})
@WithMockUser(username = "usi_admin_user", roles = {Helpers.TEAM_NAME})
public class SubmissionContentsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ValidationResultRepository validationResultRepository;

    @MockBean
    private Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap;

    @MockBean
    private DomainService domainService;
    @MockBean
    private ProfileRepositoryRest profileRepositoryRest;

    @MockBean
    private OperationControlService operationControlService;

    @MockBean
    private SubmissionRepository submissionRepository;

    @MockBean
    private LinkHelper linkHelper;


    @Autowired
    private WebApplicationContext context;

    private ValidationResult validationResult1;
    private ValidationResult validationResult2;
    private ValidationResult validationResult3;

    @Before
    public void setup() {
        Map<ValidationAuthor, List<SingleValidationResult>> validationResultByValidationAuthors = new HashMap<>();
        validationResultByValidationAuthors.putAll(generateExpectedResults(
                Arrays.asList(SingleValidationResultStatus.Pass, SingleValidationResultStatus.Error, SingleValidationResultStatus.Warning)));

        validationResult1 = generateValidationResult(validationResultByValidationAuthors);

        validationResultByValidationAuthors.clear();
        validationResultByValidationAuthors.putAll(generateExpectedResults(
                Arrays.asList(SingleValidationResultStatus.Pass, SingleValidationResultStatus.Error, SingleValidationResultStatus.Error)));

        validationResult2 = generateValidationResult(validationResultByValidationAuthors);

        validationResultByValidationAuthors.clear();
        validationResultByValidationAuthors.putAll(generateExpectedResults(
                Arrays.asList(SingleValidationResultStatus.Pass, SingleValidationResultStatus.Pass, SingleValidationResultStatus.Pass)));

        validationResult3 = generateValidationResult(validationResultByValidationAuthors);

        this.mvc = MockMvcBuilders.webAppContextSetup(context)
                .defaultRequest(RestDocumentationRequestBuilders.get("/").contextPath("/api"))
                .build();

        ApiIntegrationTestHelper.mockAapProfileAndDomain(domainService, profileRepositoryRest);
    }

    @Test
    public void whenSubmissionHas2SamplesWith1ErrorAnd1Warning_ReturnsCorrectResult() throws Exception {
        given(validationResultRepository.findBySubmissionIdAndDataTypeId(any(), any()))
                .willReturn(Stream.of(validationResult1, validationResult2, validationResult3));

        mvc.perform(get(String.format("/api/submissions/%s/contents/%s/summary", SUBMISSION_ID, SAMPLES_DATA_TYPE_ID))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNumber", is(equalTo(3))))
                .andExpect(jsonPath("$.hasError", is(equalTo(2))))
                .andExpect(jsonPath("$.hasWarning", is(equalTo(1))));
    }
}
