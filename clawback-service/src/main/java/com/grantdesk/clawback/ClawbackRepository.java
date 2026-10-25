package com.grantdesk.clawback;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClawbackRepository extends JpaRepository<ClawbackEntity, String> {

}
