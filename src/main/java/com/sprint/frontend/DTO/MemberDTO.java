package com.sprint.frontend.DTO;

public class MemberDTO {

    private int memberNumber;
    private String name;
    private String tableName; // "table name" shown on the card
    private String imageUrl; // absolute URL or /static/... path; can be null/empty

    // ── Constructors ──────────────────────────────────────────────────
    public MemberDTO() {
    }

    public MemberDTO(int memberNumber, String name, String tableName, String imageUrl) {
        this.memberNumber = memberNumber;
        this.name = name;
        this.tableName = tableName;
        this.imageUrl = imageUrl;
    }

    // ── Getters & Setters ─────────────────────────────────────────────
    public int getMemberNumber() {
        return memberNumber;
    }

    public void setMemberNumber(int memberNumber) {
        this.memberNumber = memberNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
