package com.arnovaneetvelde.blenotificator;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class SecondaryActivity extends AppCompatActivity {

    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner btScanner;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private Button butStart, butStop;
    private ListView listDevices;
    private boolean permissions;
    private ArrayList<BLEDevice> devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        permissions = false;
        btPermissions();

        butStart = (Button) findViewById(R.id.butStart);
        butStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btPermissions();
                if (permissions){
                    startScanning();
                }
            }
        });

        butStop = (Button) findViewById(R.id.butStop);
        butStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        butStop.setVisibility(View.INVISIBLE);

        devices = new ArrayList<>();

        listDevices = (ListView) findViewById(R.id.listDevices);
    }

    class CustomAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = getLayoutInflater().inflate(R.layout.custom_layout_devices,null);

            TextView textName = (TextView)view.findViewById(R.id.textName);
            TextView textDescription = (TextView)view.findViewById(R.id.textDescription);

            textName.setText("Name: " + devices.get(i).getName() + " | Saved: " + devices.get(i).isSaved().toString());
            textDescription.setText("Address: " + devices.get(i).getAddress() + " | Rssi: " + devices.get(i).getRssi().toString());

            return view;
        }
    }

    public void updateListView(){
        CustomAdapter arrayAdapter = new CustomAdapter();
        listDevices.setAdapter(arrayAdapter);
        listDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BLEDevice device = devices.get(i);
                if (device.isSaved()){
                    device.unsave();
                } else {
                    saveDevice(device);
                }
                updateListView();
            }
        });
    }

    public void saveDevice(final BLEDevice device){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (!isFinishing()){

                    final EditText inputName = new EditText(SecondaryActivity.this);

                    new android.app.AlertDialog.Builder(SecondaryActivity.this)
                            .setTitle("Save device")
                            .setMessage("Name: ")
                            .setCancelable(true)
                            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    device.setName(inputName.getText().toString());
                                    device.save();
                                    updateListView();
                                }
                            }).setView(inputName).show();
                }
            }
        });
    }

    public void btPermissions(){
        if(!permissions) {
            if (btAdapter != null && !btAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }

            // Make sure we have access coarse location enabled, if not, prompt the user to enable it
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }

            // Make sure we have access fine location enabled, if not, prompt the user to enable it
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
        }
        checkPermissions();
    }

    public void checkPermissions(){
        if (hasBlePermissions() && areLocationServicesEnabled(this)){
            permissions = true;
        } else {
            permissions = false;
            Toast.makeText(getApplicationContext(), "Please enable the required permissions.", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean hasBlePermissions() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    public boolean areLocationServicesEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void startScanning(){
        butStart.setVisibility(View.INVISIBLE);
        butStop.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning(){
        butStart.setVisibility(View.VISIBLE);
        butStop.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    public void goToMainPage(View v){
        startActivity(new Intent(this, MainActivity.class));
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            boolean exists = false;
            for (BLEDevice device : devices){
                if (device.isEqual(result.getDevice().getAddress())){
                    device.setRssi(result.getRssi());
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                BLEDevice foundDevice = new BLEDevice(result.getDevice().getName(), result.getDevice().getAddress(), result.getRssi(), getApplicationContext());
                devices.add(foundDevice);
            }
            updateListView();
        }
    };
}