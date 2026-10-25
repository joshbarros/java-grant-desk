package com.grantdesk.draw;

import com.grantdesk.common.AwardOpened;
import com.grantdesk.common.Channels;
import com.grantdesk.common.GranteeOpened;
import com.grantdesk.common.MilestoneOpened;
import com.grantdesk.common.ProgramOpened;
import com.grantdesk.common.SponsorOpened;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DrawProjection {

    private final SponsorDirectoryRepository sponsors;
    private final ProgramDirectoryRepository programs;
    private final GranteeDirectoryRepository grantees;
    private final MilestoneDirectoryRepository milestones;
    private final AwardDirectoryRepository awards;

    public DrawProjection(SponsorDirectoryRepository sponsors, ProgramDirectoryRepository programs,
            GranteeDirectoryRepository grantees, MilestoneDirectoryRepository milestones, AwardDirectoryRepository awards) {
        this.sponsors = sponsors;
        this.programs = programs;
        this.grantees = grantees;
        this.milestones = milestones;
        this.awards = awards;
    }

    @KafkaListener(topics = Channels.SPONSORS, groupId = "draw-sponsors")
    @Transactional
    public void onSponsor(SponsorOpened event) {
        if (sponsors.existsById(event.sponsorId())) {
            return;
        }
        SponsorDirectory row = new SponsorDirectory();
        row.setId(event.sponsorId());
        row.setSponsorCode(event.sponsorCode());
        row.setName(event.name());
        sponsors.save(row);
    }

    @KafkaListener(topics = Channels.PROGRAMS, groupId = "draw-programs")
    @Transactional
    public void onProgram(ProgramOpened event) {
        if (programs.existsById(event.programId())) {
            return;
        }
        ProgramDirectory row = new ProgramDirectory();
        row.setId(event.programId());
        row.setProgramCode(event.programCode());
        row.setStatus(event.status());
        programs.save(row);
    }

    @KafkaListener(topics = Channels.GRANTEES, groupId = "draw-grantees")
    @Transactional
    public void onGrantee(GranteeOpened event) {
        if (grantees.existsById(event.granteeId())) {
            return;
        }
        GranteeDirectory row = new GranteeDirectory();
        row.setId(event.granteeId());
        row.setGranteeCode(event.granteeCode());
        row.setName(event.name());
        grantees.save(row);
    }

    @KafkaListener(topics = Channels.MILESTONES, groupId = "draw-milestones")
    @Transactional
    public void onMilestone(MilestoneOpened event) {
        if (milestones.existsById(event.milestoneId())) {
            return;
        }
        MilestoneDirectory row = new MilestoneDirectory();
        row.setId(event.milestoneId());
        row.setProgramId(event.programId());
        row.setMilestoneCode(event.milestoneCode());
        row.setStatus(event.status());
        milestones.save(row);
    }

    @KafkaListener(topics = Channels.AWARDS, groupId = "draw-awards")
    @Transactional
    public void onAward(AwardOpened event) {
        if (awards.existsById(event.awardId())) {
            return;
        }
        AwardDirectory row = new AwardDirectory();
        row.setId(event.awardId());
        row.setSponsorId(event.sponsorId());
        row.setProgramId(event.programId());
        row.setAwardCode(event.awardCode());
        row.setCeilingCents(event.ceilingCents());
        row.setUnitCents(event.unitCents());
        awards.save(row);
    }
}
