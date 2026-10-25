package com.grantdesk.voucher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.Channels;
import com.grantdesk.common.CutCommand;
import com.grantdesk.common.VoucherEvent;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VoucherWorker {

    private final VoucherRepository vouchers;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public VoucherWorker(VoucherRepository vouchers, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.vouchers = vouchers;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.CUT)
    @Transactional
    public void onCut(CutCommand command) {
        if (!InboxClaims.claim(em, command.drawId() + ":cut")) {
            return;
        }
        VoucherEntity row = new VoucherEntity();
        row.setId(command.drawId());
        row.setAmountCents(command.amountCents());
        row.setStatus("ISSUED");
        vouchers.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.VOUCHERS, command.drawId(),
                new VoucherEvent(command.drawId(), row.getAmountCents(), "ISSUED"));
    }
}
