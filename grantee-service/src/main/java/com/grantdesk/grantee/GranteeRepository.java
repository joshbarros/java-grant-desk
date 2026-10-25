package com.grantdesk.grantee;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GranteeRepository extends JpaRepository<GranteeEntity, String> {
boolean existsByGranteeCode(String granteeCode);
}
