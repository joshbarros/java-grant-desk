package com.grantdesk.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.Channels;
import com.grantdesk.common.DecideCommand;
import com.grantdesk.common.DecisionEvent;
import com.grantdesk.common.GrantRules;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ComplianceWorker {

    private final DecisionRepository decisions;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;
    private final TransactionTemplate tx;

    public ComplianceWorker(DecisionRepository decisions, OutboxRepository outbox, ObjectMapper mapper, EntityManager em,
            PlatformTransactionManager manager) {
        this.decisions = decisions;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
        this.tx = new TransactionTemplate(manager);
    }

    @RabbitListener(queues = Channels.DECIDE)
    public void onDecide(DecideCommand command) {
        Boolean dead = tx.execute(status -> {
            if (!InboxClaims.claim(em, command.drawId() + ":decide")) {
                return false;
            }
            boolean lapsed = GrantRules.lapsedGrantee(command.granteeCode());
            DecisionEntity row = new DecisionEntity();
            row.setId(command.drawId());
            row.setStatus(lapsed ? "REJECTED" : "ACCEPTED");
            decisions.save(row);
            OutboxWriter.kafka(outbox, mapper, Channels.DECISIONS, command.drawId(),
                    new DecisionEvent(command.drawId(), row.getStatus()));
            return lapsed;
        });
        if (Boolean.TRUE.equals(dead)) {
            throw new AmqpRejectAndDontRequeueException("lapse");
        }
    }
}
