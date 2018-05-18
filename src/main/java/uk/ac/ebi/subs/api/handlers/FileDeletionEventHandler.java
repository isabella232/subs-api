package uk.ac.ebi.subs.api.handlers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.model.FileDeleteMessage;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

@Component
@RequiredArgsConstructor
@RepositoryEventHandler(File.class)
public class FileDeletionEventHandler {

    @NonNull
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @NonNull
    private ValidationResultRepository validationResultRepository;

    private final String FILE_DELETION_ROUTING_KEY = "usi.file.deletion";

    @HandleAfterDelete
    void handleAfterFileDeletion(File file) {

        deleteRelatedValidationResult(file);

        sendFileDeletionMessage(file);
    }

    private void sendFileDeletionMessage(File file) {
        FileDeleteMessage fileDeleteMessage = new FileDeleteMessage(
                file.getTargetPath(),
                file.getSubmissionId()
        );

        rabbitMessagingTemplate.convertAndSend(
                Exchanges.SUBMISSIONS,
                FILE_DELETION_ROUTING_KEY,
                fileDeleteMessage);
    }

    private void deleteRelatedValidationResult(File deletedFile) {
        ValidationResult validationResult = deletedFile.getValidationResult();

        validationResultRepository.delete(validationResult);
    }
}
