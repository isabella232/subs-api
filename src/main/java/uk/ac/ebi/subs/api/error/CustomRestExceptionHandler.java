package uk.ac.ebi.subs.api.error;

import org.springframework.data.rest.core.RepositoryConstraintViolationException;
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

@ControllerAdvice
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {

    private final String HTTP_STATUS_CODES = "/api/docs/submission_api.html#_http_status_codes";
    private final String SUBMISSIONS = "/api/docs/submission_api.html#_submissions";
    private final String SUBMITTABLES = "/api/docs/submission_api.html#_submittable_resources";

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

        ApiError apiError = new ApiError(HTTP_STATUS_CODES, HttpStatus.METHOD_NOT_ALLOWED, request.getDescription(false), builder.toString());
        return new ResponseEntity<>(apiError, getContentTypeHeaders(), HttpStatus.METHOD_NOT_ALLOWED);
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

        ApiError apiError = new ApiError(HTTP_STATUS_CODES, HttpStatus.BAD_REQUEST, request.getDescription(false), error);
        return new ResponseEntity<>(apiError, getContentTypeHeaders(), HttpStatus.BAD_REQUEST);
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
        ex.getErrors().getAllErrors().forEach(error -> {
            if (error.getClass().isAssignableFrom(FieldError.class)) {
                errors.add(buildErrorString((FieldError) error));
            } else {
                errors.add(error.toString());
            }
        });

        ApiError apiError;

        if (request.getDescription(false).endsWith("/api/submissions")) {
            apiError = new ApiError(SUBMISSIONS, HttpStatus.BAD_REQUEST, request.getDescription(false), errors);
        } else {
            apiError = new ApiError(SUBMITTABLES, HttpStatus.BAD_REQUEST, request.getDescription(false), errors);
        }
        return new ResponseEntity<>(apiError, getContentTypeHeaders(), HttpStatus.BAD_REQUEST);
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
        builder.append(" , field " + error.getField());
        builder.append(" can't accept value [" + error.getRejectedValue() + "].");

        return builder.toString();
    }
}
