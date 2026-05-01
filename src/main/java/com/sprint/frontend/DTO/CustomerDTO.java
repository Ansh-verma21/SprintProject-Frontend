package com.sprint.frontend.DTO;

public class CustomerDTO {
    private String fullName;
    private String email;
    private boolean active;

    public CustomerDTO() {}

    public CustomerDTO(String fullName, String email, boolean active) {
        this.fullName = fullName;
        this.email = email;
        this.active = active;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
