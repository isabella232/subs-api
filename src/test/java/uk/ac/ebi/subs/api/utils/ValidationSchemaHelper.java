package uk.ac.ebi.subs.api.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.subs.repository.model.Checklist;
import uk.ac.ebi.subs.repository.repos.schema.ValidationSchema;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ValidationSchemaHelper {

    private static final List<String> DATA_TYPE_IDS = IntStream.range(1, 31).boxed()
            .map(index -> "dataTypeId_" + index).collect(Collectors.toList());

    private static Map<String, String> SCHEMA_IDS = DATA_TYPE_IDS.stream()
            .collect(Collectors.toMap(dataTypeId -> dataTypeId, dataTypeId -> "schema_for_" + dataTypeId));

    private static final String VALIDATION_SCHEMA_PATH_AND_PREFIX = "testResources/validationSchemaFor-dataTypeId.json";

    private static List<Checklist> checklists = new ArrayList<>();

    private static ObjectMapper mapper = new ObjectMapper();

    public static List<ValidationSchema> initialiseValidationSchemas() throws IOException {
        checklists = generateMockChecklists();

        List<ValidationSchema> validationSchemas = new ArrayList<>();

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

        return validationSchemas;
    }

    public static List<Checklist> generateMockChecklists() {
        return DATA_TYPE_IDS.stream()
                .map(dataTypeId ->
                        generateChecklist(dataTypeId, SCHEMA_IDS.get(dataTypeId), generateValidationSchema(SCHEMA_IDS.get(dataTypeId))))
                .collect(Collectors.toList());
    }

    private static Checklist generateChecklist(String dataTypeId, String schemaId, JsonNode validationSchema) {
        Checklist checklist = new Checklist();
        checklist.setId(schemaId);
        checklist.setDataTypeId(dataTypeId);
        checklist.setDisplayName("Display name of " + schemaId);
        checklist.setDescription("Description of " + schemaId);
        checklist.setValidationSchema(validationSchema);

        return checklist;
    }


    private static JsonNode generateValidationSchema(String schemaId) {
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

}
