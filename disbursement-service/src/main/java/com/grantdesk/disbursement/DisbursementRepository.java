package com.grantdesk.disbursement;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DisbursementRepository extends JpaRepository<DisbursementEntity, String> {

}
