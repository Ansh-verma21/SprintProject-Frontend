package com.sprint.frontend.DTO;

public class StoreOptionDTO {
    private String label;
    private String href;
 
    public StoreOptionDTO() {}
 
    public StoreOptionDTO(String label, String href) {
        this.label = label;
        this.href  = href;
    }
 
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
 
    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }
}
