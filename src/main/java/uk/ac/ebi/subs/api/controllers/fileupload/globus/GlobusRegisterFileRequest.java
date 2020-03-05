package uk.ac.ebi.subs.api.controllers.fileupload.globus;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GlobusRegisterFileRequest {

    private List<String> files = new ArrayList<>();
}
