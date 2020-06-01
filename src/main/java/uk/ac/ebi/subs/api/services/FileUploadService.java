package uk.ac.ebi.subs.api.services;

import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.api.model.fileupload.globus.GlobusShareRequest;
import uk.ac.ebi.subs.api.model.fileupload.globus.GlobusUploadedFilesNotification;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.messaging.Topics;

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
        //It's raw version below is a quick workaround without the need to look into message re-configurations.
        Message resp = rabbitMessagingTemplate.sendAndReceive(Exchanges.SUBMISSIONS, Topics.GLOBUS_SHARE_REQUEST,
                rabbitMessagingTemplate.getMessageConverter().toMessage(req, null));
        if (resp == null) {
            throw new RuntimeException("Share not returned.");
        }

        return (String)resp.getPayload();
    }

    public void notifyUploadedFiles(String owner, String submissionId, List<String> files) {
        GlobusUploadedFilesNotification msg = new GlobusUploadedFilesNotification();
        msg.setOwner(owner);
        msg.setSubmissionId(submissionId);
        msg.setFiles(files);

        rabbitMessagingTemplate.convertAndSend(Exchanges.SUBMISSIONS, Topics.GLOBUS_UPLOADED_FILES_NOTIFICATION, msg);
    }
}
