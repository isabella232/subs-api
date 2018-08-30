package uk.ac.ebi.subs.api.services;

import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.data.submittable.BaseSubmittable;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.validator.data.ValidationEnvelopeToCoordinator;


/**
 * Created by rolando on 09/06/2017.
 */
@Component
public class SubmittableValidationDispatcher {

    @Autowired
    private RabbitMessagingTemplate rabbitMessagingTemplate;
    private static final String SUBMITTABLE_CREATED_BASE = "usi.submittable.created.";
    private static final String SUBMITTABLE_UPDATED_BASE = "usi.submittable.updated.";


    public RabbitMessagingTemplate getRabbitMessagingTemplate() {
        return rabbitMessagingTemplate;
    }

    public SubmittableValidationDispatcher() {
    }

    public void setRabbitMessagingTemplate(RabbitMessagingTemplate rabbitMessagingTemplate) {
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
    }

    public SubmittableValidationDispatcher(RabbitMessagingTemplate rabbitMessagingTemplate) {
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
    }

    public void validateCreate(StoredSubmittable storedSubmittable) {
        sendEvent(storedSubmittable, SUBMITTABLE_CREATED_BASE);
    }

    public void validateUpdate(StoredSubmittable storedSubmittable) {
        sendEvent(storedSubmittable, SUBMITTABLE_UPDATED_BASE);
    }

    protected void sendEvent(StoredSubmittable storedSubmittable, String routingKeyPrefix) {
        ensureBaseSubmittable(storedSubmittable);

        ValidationEnvelopeToCoordinator<BaseSubmittable> validationEnvelope = new ValidationEnvelopeToCoordinator(
                storedSubmittable.getSubmission().getId(),
                (BaseSubmittable) storedSubmittable,
                storedSubmittable.getDataType().getId()
        );

        String routingKey = routingKeyPrefix + submittableQueueSuffix(storedSubmittable);


        rabbitMessagingTemplate.convertAndSend(
                Exchanges.SUBMISSIONS,
                routingKey,
                validationEnvelope);
    }


    protected String submittableQueueSuffix(StoredSubmittable storedSubmittable) {
        return storedSubmittable.getClass().getSimpleName().toLowerCase();
    }

    protected void ensureBaseSubmittable(StoredSubmittable storedSubmittable) {
        if (!BaseSubmittable.class.isAssignableFrom(storedSubmittable.getClass())) {
            throw new IllegalArgumentException("StoredSubmittable should also be a base submittable");
        }
    }


}
