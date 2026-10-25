package com.grantdesk.program;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<ProgramEntity, String> {
boolean existsByProgramCode(String programCode);
}
