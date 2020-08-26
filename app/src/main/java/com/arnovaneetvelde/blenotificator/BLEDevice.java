package com.arnovaneetvelde.blenotificator;

public class BLEDevice {

    private String name, address;
    private Integer rssi;

    public BLEDevice (String name, String address, Integer rssi){
        this.name = name;
        this.address = address;
        this.rssi = rssi;
    }

    public void setRssi(Integer newRssi){
        this.rssi = newRssi;
    }

    public boolean isEqual(String compareAddress){
        return compareAddress.equals(address);
    }
}