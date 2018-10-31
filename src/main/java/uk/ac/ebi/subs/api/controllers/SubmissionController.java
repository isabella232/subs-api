package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@BasePathAwareController
public class SubmissionController {

    /**
     * This method ensures the HTTP method OPTIONS returns the correct set
     * of allowed methods for the submissions/{id} endpoint: GET, DELETE, OPTIONS
     *
     * @return the correct set of allowed methods for the submissions/{id} endpoint: GET, DELETE, OPTIONS
     */
    @RequestMapping(path = "submissions/{id}", method = RequestMethod.OPTIONS, produces = MediaTypes.HAL_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> findSubmissionOptions() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAllow(Stream.of(HttpMethod.GET, HttpMethod.DELETE, HttpMethod.OPTIONS).collect(Collectors.toCollection(LinkedHashSet::new)));
        return new ResponseEntity(headers, HttpStatus.OK);
    }
}
