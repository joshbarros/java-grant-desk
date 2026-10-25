package com.grantdesk.disbursement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "disbursements")
public class DisbursementEntity {
    @Id
    private String id;
    @Column(name = "amount_cents")
    private int amountCents;
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
