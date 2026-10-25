package com.grantdesk.draw;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "draw_actions")
public class DrawAction {
    @Id
    private String id;
    @Column(name = "draw_id")
    private String drawId;
    private String kind;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDrawId() { return drawId; }
    public void setDrawId(String drawId) { this.drawId = drawId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
}
