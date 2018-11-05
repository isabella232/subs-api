package uk.ac.ebi.subs.api.error;

import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This class respresents a custom exception handling for REST method calls.
 * It overrides some of the default behaviours and adds new ones.
 */
@ControllerAdvice
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {

    private final String API_ERRORS = "/api/docs/submission_api.html#_errors";

    /**
     * This method handles the {@link HttpRequestMethodNotSupportedException} and returns a useful body response
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
        builder.append(ex.getSupportedHttpMethods());

        ApiError apiError = new ApiError(API_ERRORS, HttpStatus.METHOD_NOT_ALLOWED, request.getDescription(false), builder.toString());
        return new ResponseEntity<>(apiError, getContentTypeHeaders(), HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * This method handles the {@link HttpMessageNotReadableException} (exception for malformed json) and returns
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

        ApiError apiError = new ApiError(API_ERRORS, HttpStatus.BAD_REQUEST, request.getDescription(false), error);
        return new ResponseEntity<>(apiError, getContentTypeHeaders(), HttpStatus.BAD_REQUEST);
    }

    /**
     * This method handles the {@link RepositoryConstraintViolationException} that gets thrown whenever a repository constrain is violated
     * like a missing required value and returns a useful body response of type {@link ApiError}
     *
     * @param ex
     * @param request
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler
    public ResponseEntity<Object> handleRepositoryConstraintViolationException(RepositoryConstraintViolationException ex, WebRequest request) {
        List<String> errors = new ArrayList<>();
        ex.getErrors().getAllErrors().forEach(error -> {
            if (FieldError.class.isAssignableFrom(error.getClass())) {
                errors.add(buildErrorString((FieldError) error));
            } else {
                errors.add(error.toString());
            }
        });

        ApiError apiError = new ApiError(API_ERRORS, HttpStatus.BAD_REQUEST, request.getDescription(false), errors);
        return new ResponseEntity<>(apiError, getContentTypeHeaders(), HttpStatus.BAD_REQUEST);
    }

    /**
     * This method handles the {@link ResourceNotFoundException}
     *
     * @param exception
     * @param request
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler
    public ResponseEntity<?> handleResourceNotFoundException(ResourceNotFoundException exception, WebRequest request) {
        ApiError apiError = new ApiError(API_ERRORS, HttpStatus.NOT_FOUND, request.getDescription(false), exception.getMessage());
        return new ResponseEntity<>(apiError, getContentTypeHeaders(), HttpStatus.NOT_FOUND);
    }

    private HttpHeaders getContentTypeHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaTypes.HAL_JSON);
        return headers;
    }

    private String buildErrorString(FieldError error) {
        StringBuilder builder = new StringBuilder();

        builder.append(error.getDefaultMessage() + ":");
        builder.append(" In " + error.getObjectName());
        builder.append(", field " + error.getField());
        builder.append(" can't be [" + error.getRejectedValue() + "].");

        return builder.toString();
    }
}
