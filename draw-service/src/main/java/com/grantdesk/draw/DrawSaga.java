package com.grantdesk.draw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.AckEvent;
import com.grantdesk.common.Channels;
import com.grantdesk.common.CutCommand;
import com.grantdesk.common.DecideCommand;
import com.grantdesk.common.DecisionEvent;
import com.grantdesk.common.DisbursementEvent;
import com.grantdesk.common.DrawFact;
import com.grantdesk.common.HoldEvent;
import com.grantdesk.common.PayCommand;
import com.grantdesk.common.RatingEvent;
import com.grantdesk.common.ReleaseCommand;
import com.grantdesk.common.ReserveCommand;
import com.grantdesk.common.VoucherEvent;
import com.grantdesk.common.WireCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DrawSaga {

    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public DrawSaga(OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @KafkaListener(topics = Channels.RATINGS, groupId = "draw-ratings")
    @Transactional
    public void onRating(RatingEvent event) {
        if (!InboxClaims.claim(em, event.drawId() + ":rating")) {
            return;
        }
        DrawEntity row = lock(event.drawId());
        if (row == null || !"SCORE".equals(row.getStatus())) {
            return;
        }
        if (!"RATED".equals(event.status())) {
            cancel(row, event.reason());
            return;
        }
        row.setAmountCents(event.amountCents());
        row.setStatus("RESERVING");
        fact(row);
        OutboxWriter.rabbit(outbox, mapper, Channels.RESERVE, row.getId(),
                new ReserveCommand(row.getId(), row.getAwardId(), row.getAmountCents()));
    }

    @KafkaListener(topics = Channels.HOLDS, groupId = "draw-holds")
    @Transactional
    public void onHold(HoldEvent event) {
        if (!InboxClaims.claim(em, event.drawId() + ":hold:" + event.status())) {
            return;
        }
        DrawEntity row = lock(event.drawId());
        if (row == null) {
            return;
        }
        if ("RESERVED".equals(event.status()) && "RESERVING".equals(row.getStatus())) {
            row.setStatus("COMPLYING");
            fact(row);
            OutboxWriter.rabbit(outbox, mapper, Channels.DECIDE, row.getId(),
                    new DecideCommand(row.getId(), row.getGranteeCode(), row.getAmountCents()));
            return;
        }
        if ("SHORT".equals(event.status()) && "RESERVING".equals(row.getStatus())) {
            cancel(row, "SHORT");
            return;
        }
        if ("RELEASED".equals(event.status()) && "RELEASING".equals(row.getStatus())) {
            cancel(row, "LAPSE");
            return;
        }
        if ("PAID".equals(event.status()) && "PAYING".equals(row.getStatus())) {
            row.setStatus("WIRING");
            fact(row);
            OutboxWriter.rabbit(outbox, mapper, Channels.WIRE, row.getId(),
                    new WireCommand(row.getId(), row.getAmountCents()));
            return;
        }
        if ("RETURNED".equals(event.status()) && ("PAID".equals(row.getStatus()) || "ACKNOWLEDGED".equals(row.getStatus()))) {
            row.setStatus("RETURNED");
            fact(row);
        }
    }

    @KafkaListener(topics = Channels.DECISIONS, groupId = "draw-decisions")
    @Transactional
    public void onDecision(DecisionEvent event) {
        if (!InboxClaims.claim(em, event.drawId() + ":decision")) {
            return;
        }
        DrawEntity row = lock(event.drawId());
        if (row == null || !"COMPLYING".equals(row.getStatus())) {
            return;
        }
        if ("REJECTED".equals(event.status())) {
            row.setStatus("RELEASING");
            fact(row);
            OutboxWriter.rabbit(outbox, mapper, Channels.RELEASE, row.getId(),
                    new ReleaseCommand(row.getId(), row.getAwardId(), row.getAmountCents()));
            return;
        }
        if ("ACCEPTED".equals(event.status())) {
            row.setStatus("CUTTING");
            fact(row);
            OutboxWriter.rabbit(outbox, mapper, Channels.CUT, row.getId(),
                    new CutCommand(row.getId(), row.getAmountCents()));
        }
    }

    @KafkaListener(topics = Channels.VOUCHERS, groupId = "draw-vouchers")
    @Transactional
    public void onVoucher(VoucherEvent event) {
        if (!InboxClaims.claim(em, event.drawId() + ":voucher")) {
            return;
        }
        DrawEntity row = lock(event.drawId());
        if (row == null || !"CUTTING".equals(row.getStatus()) || !"ISSUED".equals(event.status())) {
            return;
        }
        row.setStatus("PAYING");
        fact(row);
        OutboxWriter.rabbit(outbox, mapper, Channels.PAY, row.getId(),
                new PayCommand(row.getId(), row.getAwardId(), row.getAmountCents()));
    }

    @KafkaListener(topics = Channels.DISBURSEMENTS, groupId = "draw-disbursements")
    @Transactional
    public void onDisbursement(DisbursementEvent event) {
        if (!InboxClaims.claim(em, event.drawId() + ":disbursement")) {
            return;
        }
        DrawEntity row = lock(event.drawId());
        if (row == null || !"WIRING".equals(row.getStatus()) || !"WIRED".equals(event.status())) {
            return;
        }
        row.setStatus("PAID");
        fact(row);
    }

    @KafkaListener(topics = Channels.ACKS, groupId = "draw-acks")
    @Transactional
    public void onAck(AckEvent event) {
        if (!InboxClaims.claim(em, event.drawId() + ":acknowledgement")) {
            return;
        }
        DrawEntity row = lock(event.drawId());
        if (row == null || !"PAID".equals(row.getStatus()) || !"ISSUED".equals(event.status())) {
            return;
        }
        row.setStatus("ACKNOWLEDGED");
        fact(row);
    }

    private void cancel(DrawEntity row, String reason) {
        row.setStatus("CANCELLED");
        row.setReason(reason == null ? "" : reason);
        fact(row);
    }

    private void fact(DrawEntity row) {
        OutboxWriter.kafka(outbox, mapper, Channels.DRAWS, row.getId(),
                new DrawFact(row.getId(), row.getStatus(), row.getReason()));
    }

    private DrawEntity lock(String id) {
        return em.find(DrawEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    }
}
