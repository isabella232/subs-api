package uk.ac.ebi.subs.api.config;

import org.springframework.context.MessageSource;
import org.springframework.data.rest.webmvc.RepositoryRestExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;


/**
 * make RepositoryRestExceptionHandler apply to our controllers
 */
@ControllerAdvice("uk.ac.ebi.subs.api.controllers")
public class SubsRepositoryRestExceptionHandler extends RepositoryRestExceptionHandler {

    public SubsRepositoryRestExceptionHandler(MessageSource messageSource) {
        super(messageSource);
    }

}
