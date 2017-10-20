package uk.ac.ebi.subs.api.error;

import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * This method handles the HttpRequestMethodNotSupportedException and returns a useful body response
     * of type {@link ApiError}
     *
     * @param ex
     * @param headers
     * @param status
     * @param request
     * @return {@link ResponseEntity}
     */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(ex.getMethod());
        builder.append(" method is not supported for this request. Supported methods are ");
        ex.getSupportedHttpMethods().forEach(httpMethod -> builder.append(httpMethod + " "));

        ApiError apiError = new ApiError(HttpStatus.METHOD_NOT_ALLOWED, ex.getLocalizedMessage(), builder.toString());
        return new ResponseEntity<>(apiError, getContentTypeHeaders(), apiError.getHttpStatus());
    }

    /**
     * This method handles the HttpMessageNotReadableException (exception for malformed json) and returns
     * a useful body response of type {@link ApiError}
     *
     * @param ex
     * @param headers
     * @param status
     * @param request
     * @return {@link ResponseEntity}
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String error = "Malformed JSON request";

        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), error);
        return new ResponseEntity<>(apiError, getContentTypeHeaders(), apiError.getHttpStatus());
    }

    /**
     * This handles the RepositoryConstraintViolationException that gets thrown whenever a repository constrain is violated
     * like a missing required value and returns a useful body response of type {@link ApiError}
     *
     * @param ex
     * @param request
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(RepositoryConstraintViolationException.class)
    public ResponseEntity<Object> handleRepositoryConstraintViolationException(RepositoryConstraintViolationException ex, WebRequest request) {
        List<String> errors = new ArrayList<>();
        ex.getErrors().getAllErrors().forEach(error -> errors.add(error.toString()));

        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errors);
        return new ResponseEntity<>(apiError, getContentTypeHeaders(), apiError.getHttpStatus());
    }

    private HttpHeaders getContentTypeHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaTypes.HAL_JSON);
        return headers;
    }
}
