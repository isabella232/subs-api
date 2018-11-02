package uk.ac.ebi.subs.api.controllers;

import lombok.Data;

/**
 * This value object is the request body of a submission status request.
 */
@Data
public class SubmissionStatusDto {
    private String status;
}
