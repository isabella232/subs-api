package uk.ac.ebi.subs.api.services;

import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.api.model.fileupload.globus.GlobusShareRequest;
import uk.ac.ebi.subs.api.model.fileupload.globus.GlobusUploadedFilesNotification;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.messaging.Topics;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class FileUploadService {

    @Autowired
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    public String createGlobusShare(String owner, String submissionId) {
        GlobusShareRequest req = new GlobusShareRequest();
        req.setOwner(owner);
        req.setSubmissionId(submissionId);

        //RabbitMessagingTemplate.convertSendAndReceive() returns null due to unsupported mime type of the response message.
        //Same method on the underlying RabbitTemplate object is a reasonable workaround for this.
        Object resp = rabbitMessagingTemplate.getRabbitTemplate().convertSendAndReceive(
                Exchanges.SUBMISSIONS, Topics.GLOBUS_SHARE_REQUEST, (Object)req);
        if (resp == null) {
            throw new RuntimeException("Share not returned.");
        }

        return (String)resp;
    }

    public void notifyUploadedFiles(String owner, String submissionId, List<String> files) {
        GlobusUploadedFilesNotification msg = new GlobusUploadedFilesNotification();
        msg.setOwner(owner);
        msg.setSubmissionId(submissionId);
        msg.setFiles(files);

        rabbitMessagingTemplate.convertAndSend(Exchanges.SUBMISSIONS, Topics.GLOBUS_UPLOADED_FILES_NOTIFICATION, msg);
    }
}
