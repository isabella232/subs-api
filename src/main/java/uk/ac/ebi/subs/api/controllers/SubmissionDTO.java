package uk.ac.ebi.subs.api.controllers;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class SubmissionDTO {

    String name;
    String projectName;
    @JsonRawValue
    String uiData;
    String submissionPlan;

    @JsonSetter("uiData")
    public void setUiData(JsonNode uiData) {
        this.uiData = uiData.toString();
    }
}
