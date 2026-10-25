package com.grantdesk.milestone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "milestones")
public class MilestoneEntity {
    @Id
    private String id;
    @Column(name = "program_id")
    private String programId;
    @Column(name = "milestone_code", unique = true)
    private String milestoneCode;
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
    public String getMilestoneCode() { return milestoneCode; }
    public void setMilestoneCode(String milestoneCode) { this.milestoneCode = milestoneCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
