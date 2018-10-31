package uk.ac.ebi.subs.api.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;

import java.util.List;

/**
 * This is a Spring @Service component for {@link File} entity.
 */
@Service
@RequiredArgsConstructor
public class FileService {

    @NonNull
    private FileRepository fileRepository;

    public boolean allFilesBySubmissionIDReadyForArchive(String submissionID) {
        boolean allFilesReadyToArchive = true;
        List<File> uploadedFiles = fileRepository.findBySubmissionId(submissionID);

        for (File uploadedFile : uploadedFiles) {
            if (!uploadedFile.getStatus().equals(FileStatus.READY_FOR_ARCHIVE)) {
                allFilesReadyToArchive = false;
                break;
            }
        }

        return allFilesReadyToArchive;
    }

}
