package com.grantdesk.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionRepository extends JpaRepository<DecisionEntity, String> {

}
