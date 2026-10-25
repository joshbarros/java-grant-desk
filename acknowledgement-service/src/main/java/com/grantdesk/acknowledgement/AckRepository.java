package com.grantdesk.acknowledgement;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AckRepository extends JpaRepository<AckEntity, String> {

}
