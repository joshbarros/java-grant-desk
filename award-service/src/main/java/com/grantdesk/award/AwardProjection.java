package com.grantdesk.award;

import com.grantdesk.common.Channels;
import com.grantdesk.common.ProgramOpened;
import com.grantdesk.common.SponsorOpened;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AwardProjection {

    private final SponsorViewRepository sponsors;
    private final ProgramViewRepository programs;

    public AwardProjection(SponsorViewRepository sponsors, ProgramViewRepository programs) {
        this.sponsors = sponsors;
        this.programs = programs;
    }

    @KafkaListener(topics = Channels.SPONSORS, groupId = "award-sponsors")
    @Transactional
    public void onSponsor(SponsorOpened event) {
        if (sponsors.existsById(event.sponsorId())) {
            return;
        }
        SponsorView row = new SponsorView();
        row.setId(event.sponsorId());
        row.setSponsorCode(event.sponsorCode());
        row.setName(event.name());
        sponsors.save(row);
    }

    @KafkaListener(topics = Channels.PROGRAMS, groupId = "award-programs")
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
