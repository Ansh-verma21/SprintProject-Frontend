package com.sprint.frontend.DTO;

public class StaffDTO {
    private long staffId;
    private String fullName;
    private String email;
    private String username;
    private boolean active;
    private long storeId;

    public StaffDTO() {}

    public StaffDTO(long staffId, String fullName, String email, String username, boolean active, long storeId) {
        this.staffId = staffId;
        this.fullName = fullName;
        this.email = email;
        this.username = username;
        this.active = active;
        this.storeId = storeId;
    }

    public long getStaffId() { return staffId; }
    public void setStaffId(long staffId) { this.staffId = staffId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getStoreId() { return storeId; }
    public void setStoreId(long storeId) { this.storeId = storeId; }
}
