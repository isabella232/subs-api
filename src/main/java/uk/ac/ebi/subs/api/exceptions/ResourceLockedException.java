package uk.ac.ebi.subs.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


/**
 * Indicates a resource is not modifiable
 */
@ResponseStatus(HttpStatus.LOCKED)
public class ResourceLockedException extends RuntimeException {
    public ResourceLockedException(){}

    public ResourceLockedException(String message){
        super(message);
    }

    public ResourceLockedException(String message,
                                     Throwable cause){
        super(message,cause);
    }

}
