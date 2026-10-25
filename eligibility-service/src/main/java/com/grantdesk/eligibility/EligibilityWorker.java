package com.grantdesk.eligibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.Channels;
import com.grantdesk.common.GrantRules;
import com.grantdesk.common.RatingEvent;
import com.grantdesk.common.ScoreCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EligibilityWorker {

    private final RatingRepository ratings;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public EligibilityWorker(RatingRepository ratings, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.ratings = ratings;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.RATE)
    @Transactional
    public void onRate(ScoreCommand command) {
        if (!InboxClaims.claim(em, command.drawId() + ":rate")) {
            return;
        }
        if (GrantRules.emptyUnits(command.units())) {
            reject(command.drawId(), "EMPTY");
            return;
        }
        if (GrantRules.overUnits(command.units())) {
            reject(command.drawId(), "OVER");
            return;
        }
        try {
            int amount = GrantRules.amount(command.units(), command.unitCents());
            RatingEntity row = new RatingEntity();
            row.setId(command.drawId());
            row.setAmountCents(amount);
            row.setStatus("RATED");
            ratings.save(row);
            OutboxWriter.kafka(outbox, mapper, Channels.RATINGS, command.drawId(),
                    new RatingEvent(command.drawId(), amount, "RATED", ""));
        } catch (ArithmeticException ex) {
            reject(command.drawId(), "GROSS");
        }
    }

    private void reject(String drawId, String reason) {
        OutboxWriter.kafka(outbox, mapper, Channels.RATINGS, drawId, new RatingEvent(drawId, 0, "REJECTED", reason));
    }
}
