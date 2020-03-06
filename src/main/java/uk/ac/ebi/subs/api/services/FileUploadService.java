package uk.ac.ebi.subs.api.services;

import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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

        String share = rabbitMessagingTemplate.convertSendAndReceive(
                Exchanges.SUBMISSIONS, Topics.GLOBUS_SHARE_REQUEST, req, String.class);
        if (share == null) {
            throw new RuntimeException("Share not returned.");
        }

        return share;
    }

    public void notifyUploadedFiles(String owner, String submissionId, List<String> files) {
        GlobusUploadedFilesNotification msg = new GlobusUploadedFilesNotification();
        msg.setOwner(owner);
        msg.setSubmissionId(submissionId);
        msg.setFiles(files);

        rabbitMessagingTemplate.convertAndSend(Exchanges.SUBMISSIONS, Topics.GLOBUS_UPLOADED_FILES_NOTIFICATION, msg);
    }
}
