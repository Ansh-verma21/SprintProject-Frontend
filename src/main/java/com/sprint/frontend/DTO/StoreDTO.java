package com.sprint.frontend.DTO;

public class StoreDTO {

    private Long storeId;
    private Long managerStaffId;
    private String managerName;
    private String address;
    private String district;
    private String city;
    private String lastUpdate;

    public StoreDTO() {}

    public StoreDTO(Long storeId, Long managerStaffId, String managerName,
                    String address, String district, String city, String lastUpdate) {
        this.storeId = storeId;
        this.managerStaffId = managerStaffId;
        this.managerName = managerName;
        this.address = address;
        this.district = district;
        this.city = city;
        this.lastUpdate = lastUpdate;
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public Long getManagerStaffId() { return managerStaffId; }
    public void setManagerStaffId(Long managerStaffId) { this.managerStaffId = managerStaffId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }
}
