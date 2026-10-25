package com.grantdesk.award;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sponsor_views")
public class SponsorView {
    @Id
    private String id;
    @Column(name = "sponsor_code")
    private String sponsorCode;
    private String name;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSponsorCode() { return sponsorCode; }
    public void setSponsorCode(String sponsorCode) { this.sponsorCode = sponsorCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
