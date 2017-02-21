package uk.ac.ebi.subs.api.resourceAssembly;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.SubmittedItemsController;
import uk.ac.ebi.subs.data.SubmissionLinks;
import uk.ac.ebi.subs.data.component.Domain;
import uk.ac.ebi.subs.repository.model.Submission;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class DomainResourceProcessor implements ResourceProcessor<Resource<Domain>> {

    private Class<SubmittedItemsController> submittedItemsControllerClass = SubmittedItemsController.class;

    private Pageable defaultPageRequest() {
        return new PageRequest(0, 1);
    }

    public DomainResourceProcessor(RepositoryEntityLinks repositoryEntityLinks) {
        this.repositoryEntityLinks = repositoryEntityLinks;
    }

    private RepositoryEntityLinks repositoryEntityLinks;

    @Override
    public Resource<Domain> process(Resource<Domain> resource) {

        String domainName = resource.getContent().getName();
        Pageable pageable = defaultPageRequest();
        Submission s = new Submission();

        Map<String, String> expansionParams = new HashMap<>();
        expansionParams.put("domainName", domainName);

        /*

        */
        /* submissions */
        Link submissionsRel = repositoryEntityLinks.linkToSearchResource(Submission.class, "submissions").expand(expansionParams);

        resource.add(
                submissionsRel
        );

        /* submittables */
        resource.add(
                linkTo(
                        methodOn(submittedItemsControllerClass)
                                .analysisInDomain(domainName, pageable)
                ).withRel(SubmissionLinks.ANALYSIS)
        );

        resource.add(
                linkTo(
                        methodOn(submittedItemsControllerClass)
                                .assaysInDomain(domainName, pageable)
                ).withRel(SubmissionLinks.ASSAY)
        );

        resource.add(
                linkTo(
                        methodOn(submittedItemsControllerClass)
                                .assayDataInDomain(domainName, pageable)
                ).withRel(SubmissionLinks.ASSAY_DATA)
        );

        resource.add(
                linkTo(
                        methodOn(submittedItemsControllerClass)
                                .egaDacsInDomain(domainName, pageable)
                ).withRel(SubmissionLinks.EGA_DAC)
        );

        resource.add(
                linkTo(
                        methodOn(submittedItemsControllerClass)
                                .egaDacPoliciesInDomain(domainName, pageable)
                ).withRel(SubmissionLinks.EGA_DAC_POLICY)
        );

        resource.add(
                linkTo(
                        methodOn(submittedItemsControllerClass)
                                .egaDatasetsInDomain(domainName, pageable)
                ).withRel(SubmissionLinks.EGA_DATASET)
        );

        resource.add(
                linkTo(
                        methodOn(submittedItemsControllerClass)
                                .projectsInDomain(domainName, pageable)
                ).withRel(SubmissionLinks.PROJECT)
        );

        resource.add(
                linkTo(
                        methodOn(submittedItemsControllerClass)
                                .protocolsInDomain(domainName, pageable)
                ).withRel(SubmissionLinks.PROTOCOL)
        );

        resource.add(
                linkTo(
                        methodOn(submittedItemsControllerClass)
                                .samplesInDomain(domainName, pageable)
                ).withRel(SubmissionLinks.SAMPLE)
        );

        resource.add(
                linkTo(
                        methodOn(submittedItemsControllerClass)
                                .sampleGroupsInDomain(domainName, pageable)
                ).withRel(SubmissionLinks.SAMPLE_GROUP)
        );

        resource.add(
                linkTo(
                        methodOn(submittedItemsControllerClass)
                                .studiesInDomain(domainName, pageable)
                ).withRel(SubmissionLinks.STUDY)
        );


        return resource;
    }
}
