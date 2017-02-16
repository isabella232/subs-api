package uk.ac.ebi.subs.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;
import uk.ac.ebi.subs.api.resourceAssembly.SimpleResourceAssembler;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.component.Domain;
import uk.ac.ebi.subs.repository.SubmissionRepository;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@BasePathAwareController
public class DomainController {

    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private SimpleResourceAssembler<Submission> simpleResourceAssembler;

    @Autowired
    private PagedResourcesAssembler<Submission> pagedResourcesAssembler;

    @RequestMapping("/domains/{domainName}")
    public Resource<Domain> getDomain(@PathVariable String domainName) {
        //TODO this is a stub, we should make sure that the domains are real and that the user is authorised
        Domain d = new Domain();
        d.setName(domainName);

        Page<Submission> subsPage = submissionRepository.findByDomainName(domainName,new PageRequest(0,1));

        if (subsPage.getTotalElements() == 0){
            throw new ResourceNotFoundException();
            //TODO temporary check until we have real domain support
        }

        Resource<Domain> resource = new Resource<>(d);

        resource.add(
                linkTo(
                        methodOn(this.getClass()).getDomain(
                                d.getName()
                        )
                ).withSelfRel()
        );

        return resource;
    }

    @RequestMapping("/domains/{domainName}/submissions")
    public PagedResources<Resource<Submission>> domainSubmissions(
            @PathVariable String domainName,
            Pageable pageable) {
        Page<Submission> page = submissionRepository.findByDomainName(domainName, pageable);

        return pagedResourcesAssembler.toResource(page, simpleResourceAssembler);
    }

}
