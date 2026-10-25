package com.grantdesk.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEntry, String> {
java.util.List<AuditEntry> findByDrawIdOrderByRecordedAtAsc(String drawId);
}
