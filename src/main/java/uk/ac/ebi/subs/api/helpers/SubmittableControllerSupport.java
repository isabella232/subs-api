package uk.ac.ebi.subs.api.helpers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import uk.ac.ebi.subs.data.submittable.Submittable;
import uk.ac.ebi.subs.api.resourceAssembly.ProcessorBackedAssembler;
import uk.ac.ebi.subs.repository.repos.SubmittableRepository;

/**
 * Submittables will have standard controller endpoints available (e.g. find all items within a submission)
 * @param <T>
 */
public class SubmittableControllerSupport<T extends Submittable> {

    private SubmittableRepository<T> repository;
    private PagedResourcesAssembler<T> pagedResourcesAssembler;
    private ResourceProcessor<Resource<T>> resourceProcessor;
    private ProcessorBackedAssembler<T> processorBackedAssembler;

    public SubmittableControllerSupport(
            SubmittableRepository<T> repository,
            PagedResourcesAssembler<T> pagedResourcesAssembler,
            ResourceProcessor<Resource<T>> resourceProcessor,
            EntityLinks entityLinks) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.resourceProcessor = resourceProcessor;
        this.processorBackedAssembler = new ProcessorBackedAssembler<T>(resourceProcessor, entityLinks);
    }

    public PagedResources<Resource<T>> submittablesInSubmission(String submissionId, Pageable pageable) {
        Page<T> page = repository.findBySubmissionId(submissionId, pageable);

        return pagedResourcesAssembler.toResource(
                page,
                processorBackedAssembler
        );
    }
}
