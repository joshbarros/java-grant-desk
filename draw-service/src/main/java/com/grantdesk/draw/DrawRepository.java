package com.grantdesk.draw;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawRepository extends JpaRepository<DrawEntity, String> {
java.util.Optional<DrawEntity> findByRequestId(String requestId);
}
