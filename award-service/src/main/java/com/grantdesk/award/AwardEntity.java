package com.grantdesk.award;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "awards")
public class AwardEntity {
    @Id
    private String id;
    @Column(name = "sponsor_id")
    private String sponsorId;
    @Column(name = "program_id")
    private String programId;
    @Column(name = "award_code", unique = true)
    private String awardCode;
    @Column(name = "ceiling_cents")
    private int ceilingCents;
    @Column(name = "unit_cents")
    private int unitCents;
    @Column(name = "open_cents")
    private int openCents;
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSponsorId() { return sponsorId; }
    public void setSponsorId(String sponsorId) { this.sponsorId = sponsorId; }
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
    public String getAwardCode() { return awardCode; }
    public void setAwardCode(String awardCode) { this.awardCode = awardCode; }
    public int getCeilingCents() { return ceilingCents; }
    public void setCeilingCents(int ceilingCents) { this.ceilingCents = ceilingCents; }
    public int getUnitCents() { return unitCents; }
    public void setUnitCents(int unitCents) { this.unitCents = unitCents; }
    public int getOpenCents() { return openCents; }
    public void setOpenCents(int openCents) { this.openCents = openCents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
