package com.grantdesk.grantee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "grantees")
public class GranteeEntity {
    @Id
    private String id;
    @Column(name = "grantee_code", unique = true)
    private String granteeCode;
    private String name;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGranteeCode() { return granteeCode; }
    public void setGranteeCode(String granteeCode) { this.granteeCode = granteeCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
