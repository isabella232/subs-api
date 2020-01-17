package uk.ac.ebi.subs.api.validators;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.api.services.OperationControlService;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;

import java.util.Arrays;
import java.util.List;

/**
 * This class implements a Spring {@link Validator}.
 * It validates the {@link File} entity if the given file could be deleted.
 * The validation executes before the file deletion.
 * If there is a validation error, then the file is not getting deleted.
 */
@Component("beforeDeleteFileValidator")
public class FileDeleteValidator implements Validator {

    private SubmissionRepository submissionRepository;
    private OperationControlService operationControlService;

    public FileDeleteValidator(SubmissionRepository submissionRepository, OperationControlService operationControlService) {
        this.submissionRepository = submissionRepository;
        this.operationControlService = operationControlService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return File.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        File fileToDelete = (File) target;

        String submissionId = fileToDelete.getSubmissionId();
        SubsApiErrors.rejectIfEmptyOrWhitespace(errors,"submissionId");

        if (submissionId == null) {
            return;
        }

        final FileStatus fileToDeleteStatus = fileToDelete.getStatus();
        if (notDeletableFileStatuses().contains(fileToDeleteStatus)) {
            SubsApiErrors.file_is_not_in_deletable_status.addError(errors, "status");
        }

        Submission submission = submissionRepository.findById(submissionId).orElse(null);

        if (!operationControlService.isUpdateable(submission)) {
            SubsApiErrors.resource_locked.addError(errors);
        }

    }

    private List<FileStatus> notDeletableFileStatuses() {
        return Arrays.asList(FileStatus.INITIALIZED, FileStatus.UPLOADING);
    }
}
