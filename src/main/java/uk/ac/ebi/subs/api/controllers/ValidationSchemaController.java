package uk.ac.ebi.subs.api.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.Resources;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.repository.repos.ChecklistRepository;
import uk.ac.ebi.subs.repository.repos.schema.ValidationSchema;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@AllArgsConstructor
@Slf4j
public class ValidationSchemaController {

    private ResourceAssembler<ValidationSchema, Resource<ValidationSchema>> validationSchemaResourceAssembler;

    private PagedResourcesAssembler<ValidationSchema> pagedResourcesAssembler;

    private ChecklistRepository checklistRepository;

    private static final int DEFAULT_PAGE_SIZE = 20;

    @GetMapping(value = "/validationSchemas")
    public Resources<Resource<ValidationSchema>> getDetailedValidationSchemas(
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        List<ValidationSchema> validationSchemas;
        try {
            validationSchemas = checklistRepository.findAllValidationSchema();

            for (ValidationSchema schema: validationSchemas) {
                String schemaURI = linkTo(methodOn(
                        this.getClass()).getDetailedValidationSchema(schema.getId())).toString();
                schema.setValidationSchema(schemaURI);
            }
        } catch (IOException e) {
            log.error("Error occurred when trying to get the validation schemas. The cause of the error: {}", e.getMessage());
            throw new UncheckedIOException(e);
        }

        int start = pageable.getOffset();
        final int initialEndValue = start + pageable.getPageSize();
        int end = initialEndValue > validationSchemas.size() ? validationSchemas.size() : initialEndValue;
        final Page<ValidationSchema> pagedValidationSchemas = new PageImpl<>(validationSchemas.subList(start, end), pageable, validationSchemas.size());

        return pagedResourcesAssembler.toResource(pagedValidationSchemas);
    }

    @GetMapping(value = "/validationSchemas/{schemaId}")
    public JsonNode getDetailedValidationSchema(
            @PathVariable @P("schemaId") String schemaId) throws IOException {

        String schemaStr = checklistRepository.findValidationSchemaById(schemaId);

        ObjectMapper mapper = new ObjectMapper();

        return mapper.readTree(schemaStr);
    }
}
