package uk.ac.ebi.subs.api;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;
import uk.ac.ebi.subs.data.component.Domain;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@BasePathAwareController
public class DomainController {

    @RequestMapping("/domains/{domainName}")
    public Resource<Domain> getDomain(@PathVariable String domainName) throws UnsupportedEncodingException {
        //TODO this is a stub, we should make sure that the domains are real and that the user is authorised
        Domain d = new Domain();
        d.setName(domainName);

        Resource<Domain> resource = new Resource<>(d);

        String encodedName = UriUtils.encodePathSegment(d.getName(), StandardCharsets.UTF_8.name());

        resource.add(
                linkTo(
                        methodOn(this.getClass()).getDomain(
                                 d.getName()
                        )
                ).withSelfRel()
        );

        return resource;
    }

}
