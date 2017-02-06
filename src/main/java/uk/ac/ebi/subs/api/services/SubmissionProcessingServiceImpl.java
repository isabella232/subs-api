package uk.ac.ebi.subs.api.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.messaging.Topics;

/**
 * send a submission off to the rabbit exchange for processing
 */
@Service
public class SubmissionProcessingServiceImpl implements SubmissionProcessingService{

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    RabbitMessagingTemplate rabbitMessagingTemplate;

    @Autowired
    public SubmissionProcessingServiceImpl(RabbitMessagingTemplate rabbitMessagingTemplate, MessageConverter messageConverter) {
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
        this.rabbitMessagingTemplate.setMessageConverter(messageConverter);
    }

    @Override
    public void submitSubmissionForProcessing(Submission submission) {

        rabbitMessagingTemplate.convertAndSend(
                Exchanges.SUBMISSIONS,
                Topics.EVENT_SUBMISSION_SUBMITTED,
                submission
        );

        logger.warn("sent submission {}", submission.getId());
    }

    @Override
    public void deleteSubmissionContents(Submission submission) {
        rabbitMessagingTemplate.convertAndSend(
                Exchanges.SUBMISSIONS,
                Topics.EVENT_SUBMISSION_DELETED,
                submission
        );

        logger.warn("sent submission {}", submission.getId());
    }
}
