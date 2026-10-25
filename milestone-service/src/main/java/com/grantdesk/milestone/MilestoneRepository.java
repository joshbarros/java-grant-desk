package com.grantdesk.milestone;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<MilestoneEntity, String> {
boolean existsByMilestoneCode(String milestoneCode);
}
