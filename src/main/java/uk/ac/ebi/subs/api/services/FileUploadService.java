package uk.ac.ebi.subs.api.services;

import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;

@Service
public class FileUploadService {

    @Autowired
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    public String createGlobusShare(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        //todo read from Queues class.
        String share = rabbitMessagingTemplate.convertSendAndReceive(
                Exchanges.SUBMISSIONS, "usi.fu.globus.share.request", owner, String.class);
        if (share == null) {
            throw new RuntimeException("Share not returned.");
        }

        return share;
    }
}
