package uk.ac.ebi.subs.api.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FileServiceTest {

    private FileService fileService;

    @MockBean
    private FileRepository fileRepository;

    private static final String SUBMISSION_ID = "12345";

    @Before
    public void setup() {
        fileService = new FileService(fileRepository);
    }

    @Test
    public void whenSubmissionHasNoFiles_ThenCheckShouldReturnTrue() {
        when(fileRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Collections.emptyList());

        assertThat(fileService.allFilesBySubmissionIDReadyForArchive(SUBMISSION_ID), is(true));
    }

    @Test
    public void whenSubmissionHasFilesNotReadyForArchiveStatus_ThenItShouldReturnFalse() {
        when(fileRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Arrays.asList(
                createFile("test1.cram", FileStatus.READY_FOR_CHECKSUM),
                createFile("test2.cram", FileStatus.READY_FOR_ARCHIVE)
        ));

        assertThat(fileService.allFilesBySubmissionIDReadyForArchive(SUBMISSION_ID), is(false));

    }

    @Test
    public void whenSubmissionHasFilesAndAllHasReadyForArchiveStatus_ThenItShouldReturnTrue() {
        when(fileRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Arrays.asList(
                createFile("test1.cram", FileStatus.READY_FOR_ARCHIVE),
                createFile("test2.cram", FileStatus.READY_FOR_ARCHIVE)
        ));

        assertThat(fileService.allFilesBySubmissionIDReadyForArchive(SUBMISSION_ID), is(true));

    }

    private File createFile(String filename, FileStatus fileStatus) {
        File file = new File();
        file.setId(UUID.randomUUID().toString());
        file.setSubmissionId(SUBMISSION_ID);
        file.setFilename(filename);
        file.setStatus(fileStatus);

        return file;
    }
}
