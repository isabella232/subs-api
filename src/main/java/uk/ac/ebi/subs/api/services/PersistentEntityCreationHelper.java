package uk.ac.ebi.subs.api.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.webmvc.*;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class PersistentEntityCreationHelper {

    private ApplicationEventPublisher publisher;
    private RepositoryRestConfiguration config;
    private HttpHeadersPreparer headersPreparer;

    public ResponseEntity<ResourceSupport> createPersistentEntity(
            PersistentEntityResource payload,
            RootResourceInformation resourceInformation,
            PersistentEntityResourceAssembler assembler,
            String acceptHeader
    ) {

        publisher.publishEvent(new BeforeCreateEvent(payload.getContent()));
        Object savedObject = resourceInformation.getInvoker().invokeSave(payload.getContent());
        publisher.publishEvent(new AfterCreateEvent(savedObject));

        PersistentEntityResource resource = assembler.toFullResource(savedObject);
        HttpHeaders httpHeaders = buildHeaders(resource,savedObject);

        boolean returnBody = config.returnBodyOnCreate(acceptHeader);

        if (returnBody) {
            return ControllerUtils.toResponseEntity(
                    HttpStatus.CREATED,
                    httpHeaders,
                    resource
            );
        } else {
            return ControllerUtils.toEmptyResponse(
                    HttpStatus.CREATED,
                    httpHeaders
            );
        }
    }

    private HttpHeaders buildHeaders(PersistentEntityResource resource,Object savedObject) {
        HttpHeaders httpHeaders = headersPreparer.prepareHeaders(resource.getPersistentEntity(),savedObject);

        if (resource.getLink("self") != null){
            URI selfLink = URI.create(resource.getLink("self").expand().getHref());
            httpHeaders.setLocation(selfLink);
        }

        return httpHeaders;
    }

    public PersistentEntityCreationHelper(ApplicationEventPublisher publisher, RepositoryRestConfiguration config, HttpHeadersPreparer headersPreparer) {
        this.publisher = publisher;
        this.config = config;
        this.headersPreparer = headersPreparer;
    }
}
