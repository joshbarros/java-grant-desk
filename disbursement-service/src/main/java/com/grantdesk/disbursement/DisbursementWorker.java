package com.grantdesk.disbursement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.Channels;
import com.grantdesk.common.DisbursementEvent;
import com.grantdesk.common.WireCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DisbursementWorker {

    private final DisbursementRepository disbursements;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public DisbursementWorker(DisbursementRepository disbursements, OutboxRepository outbox, ObjectMapper mapper,
            EntityManager em) {
        this.disbursements = disbursements;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.WIRE)
    @Transactional
    public void onWire(WireCommand command) {
        if (!InboxClaims.claim(em, command.drawId() + ":wire")) {
            return;
        }
        DisbursementEntity row = new DisbursementEntity();
        row.setId(command.drawId());
        row.setAmountCents(command.amountCents());
        row.setStatus("WIRED");
        disbursements.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.DISBURSEMENTS, command.drawId(),
                new DisbursementEvent(command.drawId(), row.getAmountCents(), "WIRED"));
    }
}
