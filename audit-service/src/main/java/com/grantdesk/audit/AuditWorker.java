package com.grantdesk.audit;

import java.time.Instant;

import com.grantdesk.common.AckEvent;
import com.grantdesk.common.Channels;
import com.grantdesk.common.ClawbackEvent;
import com.grantdesk.common.DecisionEvent;
import com.grantdesk.common.DisbursementEvent;
import com.grantdesk.common.DrawFact;
import com.grantdesk.common.HoldEvent;
import com.grantdesk.common.RatingEvent;
import com.grantdesk.common.VoucherEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditWorker {

    private final AuditRepository entries;

    public AuditWorker(AuditRepository entries) {
        this.entries = entries;
    }

    @KafkaListener(topics = Channels.DRAWS, groupId = "audit-draws")
    @Transactional
    public void onDraw(DrawFact event) {
        add("DrawFact", event.drawId(), event.status());
    }

    @KafkaListener(topics = Channels.RATINGS, groupId = "audit-ratings")
    @Transactional
    public void onRating(RatingEvent event) {
        add("RatingEvent", event.drawId(), event.status());
    }

    @KafkaListener(topics = Channels.DECISIONS, groupId = "audit-decisions")
    @Transactional
    public void onDecision(DecisionEvent event) {
        add("DecisionEvent", event.drawId(), event.status());
    }

    @KafkaListener(topics = Channels.HOLDS, groupId = "audit-holds")
    @Transactional
    public void onHold(HoldEvent event) {
        add("HoldEvent", event.drawId(), event.status());
    }

    @KafkaListener(topics = Channels.VOUCHERS, groupId = "audit-vouchers")
    @Transactional
    public void onVoucher(VoucherEvent event) {
        add("VoucherEvent", event.drawId(), event.status());
    }

    @KafkaListener(topics = Channels.DISBURSEMENTS, groupId = "audit-disbursements")
    @Transactional
    public void onDisbursement(DisbursementEvent event) {
        add("DisbursementEvent", event.drawId(), event.status());
    }

    @KafkaListener(topics = Channels.ACKS, groupId = "audit-acks")
    @Transactional
    public void onAck(AckEvent event) {
        add("AckEvent", event.drawId(), event.status());
    }

    @KafkaListener(topics = Channels.CLAWBACKS, groupId = "audit-clawbacks")
    @Transactional
    public void onClawback(ClawbackEvent event) {
        add("ClawbackEvent", event.drawId(), event.status());
    }

    private void add(String kind, String drawId, String status) {
        String id = kind + ":" + drawId + ":" + status;
        if (entries.existsById(id)) {
            return;
        }
        AuditEntry row = new AuditEntry();
        row.setId(id);
        row.setDrawId(drawId);
        row.setKind(kind);
        row.setStatus(status);
        row.setRecordedAt(Instant.now());
        entries.save(row);
    }
}
