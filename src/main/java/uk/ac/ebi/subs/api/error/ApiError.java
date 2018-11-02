package uk.ac.ebi.subs.api.error;

import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

/**
 * A value object representing an API error.
 */
public class ApiError {

    /**
     * An URL to a document describing the error condition (optional).
     */
    private String type;
    /**
     * A brief title for the error condition (required and should be the same for every problem of the same {@link ApiError#type}.
     */
    private String title;
    /**
     * The HTTP status code for the current request (required).
     */
    private int status;
    /**
     * URI identifying the specific instance of this problem (optional).
     */
    private String instance;
    /**
     * Error details specific to this request (optional).
     */
    private List<String> errors;

    public ApiError() {}

    public ApiError(HttpStatus httpStatus) {
        this.title = httpStatus.getReasonPhrase();
        this.status = httpStatus.value();
    }

    public ApiError(String type, HttpStatus httpStatus, String instance, List<String> errors) {
        this.type = type;
        this.title = httpStatus.getReasonPhrase();
        this.status = httpStatus.value();
        this.instance = instance;
        this.errors = errors;
    }

    public ApiError(String type, HttpStatus httpStatus, String instance, String error) {
        this.type = type;
        this.title = httpStatus.getReasonPhrase();
        this.status = httpStatus.value();
        this.instance = instance;
        this.errors = Arrays.asList(error);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void setError(String error) {
        this.errors = Arrays.asList(error);
    }

}
