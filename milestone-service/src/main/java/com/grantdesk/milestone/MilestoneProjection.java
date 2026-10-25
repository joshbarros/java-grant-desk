package com.grantdesk.milestone;

import com.grantdesk.common.Channels;
import com.grantdesk.common.ProgramOpened;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MilestoneProjection {

    private final ProgramViewRepository programs;

    public MilestoneProjection(ProgramViewRepository programs) {
        this.programs = programs;
    }

    @KafkaListener(topics = Channels.PROGRAMS, groupId = "milestone-programs")
    @Transactional
    public void onProgram(ProgramOpened event) {
        if (programs.existsById(event.programId())) {
            return;
        }
        ProgramView row = new ProgramView();
        row.setId(event.programId());
        row.setProgramCode(event.programCode());
        row.setStatus(event.status());
        programs.save(row);
    }
}
