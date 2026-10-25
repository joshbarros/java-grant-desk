package com.grantdesk.draw;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "draws")
public class DrawEntity {
    @Id
    private String id;
    @Column(name = "request_id", unique = true)
    private String requestId;
    @Column(name = "sponsor_id")
    private String sponsorId;
    @Column(name = "program_id")
    private String programId;
    @Column(name = "grantee_id")
    private String granteeId;
    @Column(name = "milestone_id")
    private String milestoneId;
    @Column(name = "award_id")
    private String awardId;
    @Column(name = "grantee_code")
    private String granteeCode;
    private int units;
    @Column(name = "unit_cents")
    private int unitCents;
    @Column(name = "amount_cents")
    private int amountCents;
    private String status;
    private String reason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getSponsorId() { return sponsorId; }
    public void setSponsorId(String sponsorId) { this.sponsorId = sponsorId; }
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
    public String getGranteeId() { return granteeId; }
    public void setGranteeId(String granteeId) { this.granteeId = granteeId; }
    public String getMilestoneId() { return milestoneId; }
    public void setMilestoneId(String milestoneId) { this.milestoneId = milestoneId; }
    public String getAwardId() { return awardId; }
    public void setAwardId(String awardId) { this.awardId = awardId; }
    public String getGranteeCode() { return granteeCode; }
    public void setGranteeCode(String granteeCode) { this.granteeCode = granteeCode; }
    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }
    public int getUnitCents() { return unitCents; }
    public void setUnitCents(int unitCents) { this.unitCents = unitCents; }
    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
