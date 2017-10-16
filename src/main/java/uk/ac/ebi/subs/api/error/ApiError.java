package uk.ac.ebi.subs.api.error;

import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

public class ApiError {

    private HttpStatus httpStatus;
    private int status;
    private String message;
    private List<String> errors;

    public ApiError(HttpStatus httpStatus, String message, List<String> errors) {
        this.httpStatus = httpStatus;
        this.status = httpStatus.value();
        this.message = message;
        this.errors = errors;
    }

    public ApiError(HttpStatus httpStatus, String message, String error) {
        this.httpStatus = httpStatus;
        this.status = httpStatus.value();
        this.message = message;
        this.errors = Arrays.asList(error);
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
