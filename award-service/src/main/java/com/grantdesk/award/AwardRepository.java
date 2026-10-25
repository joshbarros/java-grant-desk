package com.grantdesk.award;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardRepository extends JpaRepository<AwardEntity, String> {
boolean existsByAwardCode(String awardCode);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE awards SET open_cents = open_cents - :amt WHERE id = :id AND open_cents >= :amt", nativeQuery = true)
    int reserve(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("amt") int amt);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE awards SET open_cents = open_cents + :amt WHERE id = :id", nativeQuery = true)
    int credit(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("amt") int amt);
}
