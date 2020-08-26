package com.arnovaneetvelde.blenotificator;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

public class BLEDevice {

    private String name, address;
    private Integer rssi, saveIndex;
    private Boolean saved;
    private final int maxSavedItems = 5;
    private Context context;

    public BLEDevice (String name, String address, Integer rssi, Context context){
        this.name = name;
        this.address = address;
        this.rssi = rssi;
        this.context = context;
        this.saved = checkIfSaved();
    }

    public String getName(){
        return name;
    }
    public String getAddress(){
        return address;
    }
    public Integer getRssi(){
        return rssi;
    }
    public Boolean isSaved(){
        return saved;
    }
    public void save(){
        saveDevice();
    }
    public void unsave(){
        saved = false;
        deleteDevice();
        //deleteAllDevices();
    }

    public void setName(String newName){
        this.name = newName;
    }
    public void setRssi(Integer newRssi){
        this.rssi = newRssi;
    }

    public boolean isEqual(String compareAddress){
        return compareAddress.equals(address);
    }

    private boolean checkIfSaved(){
        SharedPreferences settings = context.getSharedPreferences("SavedDevices", Context.MODE_PRIVATE);
        for (int i = 0; i < maxSavedItems; i++){
            if(this.address.equals(settings.getString("Address" + Integer.toString(i), null))){
                this.saveIndex = i;
                setName(settings.getString("Name" + i, null));
                return true;
            }
        }
        return false;
    }

    private void saveDevice(){
        SharedPreferences settings = context.getSharedPreferences("SavedDevices", Context.MODE_PRIVATE);
        for (int i = 0; i < maxSavedItems; i++){
            if((settings.getString("Address" + Integer.toString(i), null) == null ||
                    settings.getString("Address" + Integer.toString(i), null).equals("")) &&
                    (settings.getString("Name" + Integer.toString(i), null) == null ||
                            settings.getString("Name" + Integer.toString(i), null).equals(""))){
                SharedPreferences.Editor editor = settings.edit();
                saveDevice(editor, i);
                this.saveIndex = i;
                editor.apply();
                this.saved = true;
                return;
            } else if (this.address.equals(settings.getString("Address" + Integer.toString(i), null))){
                SharedPreferences.Editor editor = settings.edit();
                saveDevice(editor, i);
                this.saveIndex = i;
                editor.apply();
                this.saved = true;
                return;
            }
        }
        Toast.makeText(context, "You have reached the max amount of saved devices", Toast.LENGTH_SHORT).show();
    }

    private void deleteDevice(){
        SharedPreferences settings = context.getSharedPreferences("SavedDevices", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        deleteDevice(editor, saveIndex);
        for (int i = saveIndex + 1; i < maxSavedItems; i++){
            if((settings.getString("Address" + Integer.toString(i), null) == null ||
                    settings.getString("Address" + Integer.toString(i), null).equals("")) &&
                    (settings.getString("Name" + Integer.toString(i), null) == null ||
                            settings.getString("Name" + Integer.toString(i), null).equals(""))){
                break;
            } else {
                saveDevice(editor, i - 1, settings.getString("Name" + Integer.toString(i), null), settings.getString("Address" + Integer.toString(i), null));
                deleteDevice(editor, i);
            }
        }
        editor.apply();
        saveIndex = null;
    }

    private void saveDevice(SharedPreferences.Editor editor, int index, String Name, String Address){
        editor.putString("Name" + Integer.toString(index), Name);
        editor.putString("Address" + Integer.toString(index), Address);
    }

    private void saveDevice(SharedPreferences.Editor editor, int index){
        editor.putString("Name" + Integer.toString(index), this.name);
        editor.putString("Address" + Integer.toString(index), this.address);
    }

    private void deleteDevice(SharedPreferences.Editor editor, int index){
        editor.remove("Name" + index);
        editor.remove("Address" + index);
    }

    public void deleteAllDevices(){
        SharedPreferences settings = context.getSharedPreferences("SavedDevices", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.apply();
    }
}