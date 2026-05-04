package com.sprint.frontend.DTO;

public class LanguageDTO {
    private int languageId;
    private String name;
    private String lastUpdate;

    public LanguageDTO() {}

    public LanguageDTO(int languageId, String name, String lastUpdate) {
        this.languageId = languageId;
        this.name = name;
        this.lastUpdate = lastUpdate;
    }

    public int getLanguageId() { return languageId; }
    public void setLanguageId(int languageId) { this.languageId = languageId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }
}
