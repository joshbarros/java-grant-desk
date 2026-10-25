package com.grantdesk.award;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "program_views")
public class ProgramView {
    @Id
    private String id;
    @Column(name = "program_code")
    private String programCode;
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProgramCode() { return programCode; }
    public void setProgramCode(String programCode) { this.programCode = programCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
