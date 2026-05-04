package com.sprint.frontend.DTO;

public class ActorDTO {

    private String firstName;
    private String lastName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = safe(firstName);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = safe(lastName);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    private String safe(String val) {
        return (val == null || val.isEmpty()) ? "N/A" : val;
    }
}