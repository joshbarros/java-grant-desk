package com.grantdesk.award;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldRepository extends JpaRepository<HoldEntity, String> {
@org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "INSERT INTO holds (id, award_id, amount_cents, status) VALUES (:id, :awardId, :amt, 'HELD')", nativeQuery = true)
    int insertHold(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("awardId") String awardId, @org.springframework.data.repository.query.Param("amt") int amt);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE holds SET status = 'RELEASED' WHERE id = :id AND award_id = :awardId AND status = 'HELD' AND amount_cents = :amt", nativeQuery = true)
    int releaseHold(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("awardId") String awardId, @org.springframework.data.repository.query.Param("amt") int amt);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE holds SET status = 'PAID' WHERE id = :id AND award_id = :awardId AND status = 'HELD' AND amount_cents = :amt", nativeQuery = true)
    int payHold(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("awardId") String awardId, @org.springframework.data.repository.query.Param("amt") int amt);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE holds SET status = 'RETURNED' WHERE id = :id AND award_id = :awardId AND status = 'PAID' AND amount_cents = :amt", nativeQuery = true)
    int restoreHold(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("awardId") String awardId, @org.springframework.data.repository.query.Param("amt") int amt);
}
