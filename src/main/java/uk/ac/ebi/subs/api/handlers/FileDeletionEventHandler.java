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

@Component
@RequiredArgsConstructor
@RepositoryEventHandler(File.class)
public class FileDeletionEventHandler {

    @NonNull
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    private final String FILE_DELETION_ROUTING_KEY = "usi.file.deletion";

    @HandleAfterDelete
    public void sendFileDeletionMessage(File file) {
        FileDeleteMessage fileDeleteMessage = new FileDeleteMessage();
        fileDeleteMessage.setTargetFilePath(file.getTargetPath());
        rabbitMessagingTemplate.convertAndSend(
                Exchanges.SUBMISSIONS,
                FILE_DELETION_ROUTING_KEY,
                fileDeleteMessage);
    }
}
