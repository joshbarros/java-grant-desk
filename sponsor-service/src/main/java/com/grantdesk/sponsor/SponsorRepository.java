package com.grantdesk.sponsor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SponsorRepository extends JpaRepository<SponsorEntity, String> {
boolean existsBySponsorCode(String sponsorCode);
}
