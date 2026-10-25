package com.grantdesk.clawback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.Channels;
import com.grantdesk.common.ClawbackEvent;
import com.grantdesk.common.ReleaseCommand;
import com.grantdesk.common.ReleaseHoldCommand;
import com.grantdesk.common.RestoreCommand;
import com.grantdesk.common.ReturnCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClawbackWorker {

    private final ClawbackRepository clawbacks;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public ClawbackWorker(ClawbackRepository clawbacks, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.clawbacks = clawbacks;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.RELEASE)
    @Transactional
    public void onRelease(ReleaseCommand command) {
        if (!InboxClaims.claim(em, command.drawId() + ":release")) {
            return;
        }
        save(command.drawId(), command.awardId(), command.amountCents(), "RESTORED");
        OutboxWriter.rabbit(outbox, mapper, Channels.RELEASE_HOLD, command.drawId(),
                new ReleaseHoldCommand(command.drawId(), command.awardId(), command.amountCents()));
    }

    @RabbitListener(queues = Channels.RETURN)
    @Transactional
    public void onReturn(ReturnCommand command) {
        if (!InboxClaims.claim(em, command.drawId() + ":return")) {
            return;
        }
        save(command.drawId(), command.awardId(), command.amountCents(), "RETURNED");
        OutboxWriter.rabbit(outbox, mapper, Channels.RESTORE, command.drawId(),
                new RestoreCommand(command.drawId(), command.awardId(), command.amountCents()));
    }

    private void save(String drawId, String awardId, int amountCents, String status) {
        ClawbackEntity row = new ClawbackEntity();
        row.setId(drawId);
        row.setAwardId(awardId);
        row.setAmountCents(amountCents);
        row.setStatus(status);
        clawbacks.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.CLAWBACKS, drawId, new ClawbackEvent(drawId, status));
    }
}
