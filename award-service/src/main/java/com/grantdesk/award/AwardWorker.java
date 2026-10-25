package com.grantdesk.award;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.Channels;
import com.grantdesk.common.HoldEvent;
import com.grantdesk.common.PayCommand;
import com.grantdesk.common.ReleaseHoldCommand;
import com.grantdesk.common.ReserveCommand;
import com.grantdesk.common.RestoreCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AwardWorker {

    private final AwardRepository awards;
    private final HoldRepository holds;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public AwardWorker(AwardRepository awards, HoldRepository holds, OutboxRepository outbox, ObjectMapper mapper,
            EntityManager em) {
        this.awards = awards;
        this.holds = holds;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.RESERVE, concurrency = "4")
    @Transactional
    public void onReserve(ReserveCommand command) {
        if (!InboxClaims.claim(em, command.drawId() + ":reserve")) {
            return;
        }
        if (awards.reserve(command.awardId(), command.amountCents()) != 1) {
            publish(command.drawId(), command.awardId(), command.amountCents(), "SHORT");
            return;
        }
        if (holds.insertHold(command.drawId(), command.awardId(), command.amountCents()) != 1) {
            throw new IllegalStateException("hold");
        }
        publish(command.drawId(), command.awardId(), command.amountCents(), "RESERVED");
    }

    @RabbitListener(queues = Channels.RELEASE_HOLD)
    @Transactional
    public void onRelease(ReleaseHoldCommand command) {
        if (!InboxClaims.claim(em, command.drawId() + ":release-hold")) {
            return;
        }
        if (holds.releaseHold(command.drawId(), command.awardId(), command.amountCents()) != 1) {
            return;
        }
        if (awards.credit(command.awardId(), command.amountCents()) != 1) {
            throw new IllegalStateException("ceiling");
        }
        publish(command.drawId(), command.awardId(), command.amountCents(), "RELEASED");
    }

    @RabbitListener(queues = Channels.PAY)
    @Transactional
    public void onPay(PayCommand command) {
        if (!InboxClaims.claim(em, command.drawId() + ":pay")) {
            return;
        }
        if (holds.payHold(command.drawId(), command.awardId(), command.amountCents()) == 1) {
            publish(command.drawId(), command.awardId(), command.amountCents(), "PAID");
        }
    }

    @RabbitListener(queues = Channels.RESTORE)
    @Transactional
    public void onRestore(RestoreCommand command) {
        if (!InboxClaims.claim(em, command.drawId() + ":restore")) {
            return;
        }
        if (holds.restoreHold(command.drawId(), command.awardId(), command.amountCents()) != 1) {
            return;
        }
        if (awards.credit(command.awardId(), command.amountCents()) != 1) {
            throw new IllegalStateException("ceiling");
        }
        publish(command.drawId(), command.awardId(), command.amountCents(), "RETURNED");
    }

    private void publish(String drawId, String awardId, int amountCents, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.HOLDS, drawId,
                new HoldEvent(drawId, awardId, amountCents, status));
    }
}
