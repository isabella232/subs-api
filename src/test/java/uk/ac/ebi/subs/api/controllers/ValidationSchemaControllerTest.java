package uk.ac.ebi.subs.api.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.schema.ValidationSchema;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ValidationSchemaController.class)
@EnableSpringDataWebSupport
public class ValidationSchemaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private ChecklistRepository checklistRepository;

    private static Map<String, String> SCHEMA_IDS;
    private static final List<String> DATA_TYPE_IDS = IntStream.range(1, 31).boxed()
        .map(index -> "dataTypeId_" + index).collect(Collectors.toList());
    private static final String VALIDATION_SCHEMA_PATH_AND_PREFIX = "testResources/validationSchemaFor-dataTypeId.json";

    private List<Checklist> checklists = new ArrayList<>();
    private List<ValidationSchema> validationSchemas = new ArrayList<>();

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() throws IOException {
        this.mvc = MockMvcBuilders.webAppContextSetup(context)
                .defaultRequest(RestDocumentationRequestBuilders.get("/").contextPath("/api"))
                .build();

        SCHEMA_IDS = DATA_TYPE_IDS.stream()
                .collect(Collectors.toMap(dataTypeId -> dataTypeId, dataTypeId -> "schema_for_" + dataTypeId));

        generateMockChecklists();
        initialiseValidationSchemas();
    }

    @Test
    public void given30ValidationSchemaExists_queryingValidationSchemas_shouldReturnSchemaDetailsOfFirstPage() throws Exception {
        given(checklistRepository.findAllValidationSchema()).willReturn(validationSchemas);

        final String expectedSchemaId = "schema_for_dataTypeId_20";
        mvc.perform(get("/api/validationSchemas?page=0&size=20")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.validationSchemas.length()", is(equalTo(20))))
                .andExpect(jsonPath("$.page.totalPages", is(equalTo(2))))
                .andExpect(jsonPath("$.page.totalElements", is(equalTo(30))))
                .andExpect(jsonPath("$._embedded.validationSchemas[19].id", is(equalTo(expectedSchemaId))))
                .andExpect(jsonPath("$._embedded.validationSchemas[19].validationSchema", containsString(expectedSchemaId)));
    }

    @Test
    public void whenUserQuerySpecificValidationSchema_shouldReturnItsJsonSchemaWhenItExists() throws Exception {
        int schemaIdIndex = 11;
        given(checklistRepository.findValidationSchemaById(any())).willReturn(validationSchemas.get(schemaIdIndex).getValidationSchema());

        final String expectedSchemaId = "schema_for_dataTypeId_" + ++schemaIdIndex;

        mvc.perform(get(String.format("/api/validationSchemas/%s", expectedSchemaId))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(equalTo(expectedSchemaId))));
    }

    @Test
    public void whenUserQuerySpecificValidationSchema_shouldReturn404WhenItDoesNotExist() throws Exception {
        int schemaIdIndex = 9999;
        given(checklistRepository.findValidationSchemaById(any())).willReturn(null);

        final String expectedSchemaId = "schema_for_dataTypeId_" + ++schemaIdIndex;

        mvc.perform(get(String.format("/api/validationSchemas/%s", expectedSchemaId))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is(equalTo("Not Found"))))
                .andExpect(jsonPath("$.status", is(equalTo(HttpStatus.NOT_FOUND.value()))));
    }

    private void generateMockChecklists() {
        checklists = DATA_TYPE_IDS.stream()
                .map(dataTypeId ->
                        generateChecklist(dataTypeId, SCHEMA_IDS.get(dataTypeId), generateValidationSchema(SCHEMA_IDS.get(dataTypeId))))
                .collect(Collectors.toList());
    }

    private Checklist generateChecklist(String dataTypeId, String schemaId, JsonNode validationSchema) {
        Checklist checklist = new Checklist();
        checklist.setId(schemaId);
        checklist.setDataTypeId(dataTypeId);
        checklist.setDisplayName("Display name of " + schemaId);
        checklist.setDescription("Description of " + schemaId);
        checklist.setValidationSchema(validationSchema);

        return checklist;
    }

    private JsonNode generateValidationSchema(String schemaId) {
        File validationSchema =
                new File(ClassLoader.getSystemClassLoader().getResource(VALIDATION_SCHEMA_PATH_AND_PREFIX).getFile());

        JsonNode schemaResource;
        try {
            schemaResource = mapper.readTree(validationSchema);

            String target = "TO_REPLACE";

            String changedResource = schemaResource.toString().replace(target, schemaId);

            return mapper.readTree(changedResource);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file: " + VALIDATION_SCHEMA_PATH_AND_PREFIX);
        }
    }

    private void initialiseValidationSchemas() throws IOException {
        for (Checklist checklist : checklists) {
            String schemaString = checklist.getValidationSchema();
            final JsonNode schemaJson = mapper.readTree(schemaString);
            ValidationSchema schema = new ValidationSchema();
            schema.setId(schemaJson.get("id").asText());
            schema.setDataTypeId(checklist.getDataTypeId());
            schema.setDescription(checklist.getDescription());
            schema.setDisplayName(checklist.getDisplayName());
            schema.setLastModifiedDate(checklist.getLastModifiedDate());
            schema.setValidationSchema(checklist.getValidationSchema());

            validationSchemas.add(schema);
        }
    }
}
