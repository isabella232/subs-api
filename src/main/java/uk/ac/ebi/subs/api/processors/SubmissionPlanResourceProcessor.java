package uk.ac.ebi.subs.api.processors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.model.SubmissionPlanResource;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.SubmissionPlan;
import uk.ac.ebi.subs.repository.repos.DataTypeRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * EntityModel processor for {@link SubmissionPlan} entity used by Spring MVC controller.
 */
@Component
@RequiredArgsConstructor
public class SubmissionPlanResourceProcessor implements RepresentationModelProcessor<EntityModel<SubmissionPlan>> {

    @NonNull
    private DataTypeRepository dataTypeRepository;

    @Override
    public EntityModel<SubmissionPlan> process(EntityModel<SubmissionPlan> resource) {
        SubmissionPlanResource submissionPlanResource = new SubmissionPlanResource(resource);

        SubmissionPlan submissionPlan = resource.getContent();

        List<DataType> dataTypes = submissionPlan.getDataTypeIds()
                .stream()
                .map(id -> dataTypeRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        submissionPlanResource.setDataTypes(dataTypes);

        return submissionPlanResource;
    }
}
