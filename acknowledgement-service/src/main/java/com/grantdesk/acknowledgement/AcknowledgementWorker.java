package com.grantdesk.acknowledgement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.AckCommand;
import com.grantdesk.common.AckEvent;
import com.grantdesk.common.Channels;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AcknowledgementWorker {

    private final AckRepository acknowledgements;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public AcknowledgementWorker(AckRepository acknowledgements, OutboxRepository outbox, ObjectMapper mapper,
            EntityManager em) {
        this.acknowledgements = acknowledgements;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.ACK)
    @Transactional
    public void onAck(AckCommand command) {
        if (!InboxClaims.claim(em, command.drawId() + ":ack")) {
            return;
        }
        AckEntity row = new AckEntity();
        row.setId(command.drawId());
        row.setStatus("ISSUED");
        acknowledgements.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.ACKS, command.drawId(), new AckEvent(command.drawId(), "ISSUED"));
    }
}
