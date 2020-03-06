package uk.ac.ebi.subs.api.controllers.fileupload.globus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.services.FileUploadService;
import uk.ac.ebi.subs.api.validators.SubsApiErrors;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.fileupload.GlobusShare;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.fileupload.GlobusShareRepository;

@RestController
public class GlobusFileUploadController {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private GlobusShareRepository globusShareRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping(value = "/fileupload/globus/{submissionId}/share")
    public ResponseEntity<GlobusShareResponse> createGlobusShare(@PathVariable @P("submissionId") String submissionId) {

        Submission submission = submissionRepository.findOne(submissionId);
        if (submission == null) {
            throw new ResourceNotFoundException();
        }

        GlobusShareResponse resp = new GlobusShareResponse();
        resp.setShareLink(fileUploadService.createGlobusShare(submission.getCreatedBy(), submissionId));

        return ResponseEntity.ok(resp);
    }

    @PutMapping(value = "/fileupload/globus/{submissionId}/uploadedFiles")
    public ResponseEntity<Void> notifyUploadedFiles(
            @PathVariable @P("submissionId") String submissionId,
            @RequestBody GlobusRegisterFileRequest request,
            BindingResult bindingResult) {

        if (request == null || request.getFiles() == null || request.getFiles().isEmpty()) {
            SubsApiErrors.rejectIfEmptyOrWhitespace(bindingResult, "files");
        }

        Submission submission = submissionRepository.findOne(submissionId);
        if (submission == null) {
            throw new ResourceNotFoundException();
        }

        String owner = submission.getCreatedBy();
        GlobusShare gs = globusShareRepository.findOne(owner);
        if (gs == null || gs.getRegisteredSubmissionIds().stream().noneMatch(regSubId -> regSubId.equals(submissionId))) {
            throw new ResourceNotFoundException("Share not found.");
        }

        fileUploadService.notifyUploadedFiles(submission.getCreatedBy(), submissionId, request.getFiles());

        return ResponseEntity.ok().build();
    }
}
