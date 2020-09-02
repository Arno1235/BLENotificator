package com.arnovaneetvelde.blenotificator;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner btScanner;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private Button butON, butOFF;
    private ListView listSavedDevices;
    private boolean permissions;
    private ArrayList<BLEDevice> savedDevices;
    private final int maxSavedItems = 5;
    private NotificationManager notificationManager;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        permissions = false;
        btPermissions();

        butON = (Button) findViewById(R.id.butON);
        butON.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btPermissions();
                if (permissions){
                    startDetection();
                }
            }
        });

        butOFF = (Button) findViewById(R.id.butOFF);
        butOFF.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopDetection();
                stopDetection();
                stopDetection();
            }
        });
        butOFF.setVisibility(View.INVISIBLE);

        savedDevices = new ArrayList<>();

        listSavedDevices = (ListView) findViewById(R.id.listSavedDevices);

        notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager.getNotificationChannel("BLENotificator") == null){
            createNotificationChannel();
        }
        if (notificationManager.getNotificationChannel("BLENotificatorBackgroundService") == null){
            createBGNotificationChannel();
        }

        updateListView();

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    class CustomAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return savedDevices.size();
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

            textName.setText("Name: " + savedDevices.get(i).getName() + " | Saved: " + savedDevices.get(i).isSaved().toString());
            textDescription.setText("Address: " + savedDevices.get(i).getAddress());

            return view;
        }
    }

    public void updateListView(){
        updateDevicesList();
        MainActivity.CustomAdapter arrayAdapter = new MainActivity.CustomAdapter();
        listSavedDevices.setAdapter(arrayAdapter);
        listSavedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BLEDevice device = savedDevices.get(i);
                devicePopUp(device);
                updateListView();
            }
        });
    }

    public void updateDevicesList(){
        SharedPreferences settings = getApplicationContext().getSharedPreferences("SavedDevices", Context.MODE_PRIVATE);
        savedDevices.clear();
        for (int i = 0; i < maxSavedItems; i++){
            if((settings.getString("Address" + Integer.toString(i), null) != null &&
                    !settings.getString("Address" + Integer.toString(i), null).equals("")) ||
                    (settings.getString("Name" + Integer.toString(i), null) != null &&
                            !settings.getString("Name" + Integer.toString(i), null).equals(""))) {
                BLEDevice device = new BLEDevice(settings.getString("Name" + i, null), settings.getString("Address" + i, null), 0, getApplicationContext());
                savedDevices.add(device);
            }
        }
    }

    public void devicePopUp(final BLEDevice device){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (!isFinishing()){

                    final EditText inputName = new EditText(MainActivity.this);

                    new android.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle(device.getName())
                            .setMessage("Change name: ")
                            .setCancelable(true)
                            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    device.setName(inputName.getText().toString());
                                    device.save();
                                    updateListView();
                                }
                            }).setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            device.unsave();
                            updateListView();
                        }
                    }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

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

    public void goToSecondaryPage(View v){
        startActivity(new Intent(this, SecondaryActivity.class));
    }

    public void startDetection(){
        butON.setVisibility(View.INVISIBLE);
        butOFF.setVisibility(View.VISIBLE);
        SharedPreferences BGSettings = getApplicationContext().getSharedPreferences("BackgroundService", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = BGSettings.edit();
        editor.putBoolean("Run", true);
        editor.apply();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, BackgroundService.class));
        } else {
            startService(new Intent(this, BackgroundService.class));
        }
    }
    public void stopDetection(){
        butON.setVisibility(View.VISIBLE);
        butOFF.setVisibility(View.INVISIBLE);
        SharedPreferences BGSettings = getApplicationContext().getSharedPreferences("BackgroundService", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = BGSettings.edit();
        editor.putBoolean("Run", false);
        editor.apply();
        stopService(new Intent(this, BackgroundService.class));
    }

    /**
    public void startDetection(){
        butON.setVisibility(View.INVISIBLE);
        butOFF.setVisibility(View.VISIBLE);
        SharedPreferences BGSettings = getApplicationContext().getSharedPreferences("BackgroundService", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = BGSettings.edit();
        editor.putBoolean("Run", true);
        editor.apply();
        startService(new Intent(this, BackgroundService.class));
    }

    public void stopDetection(){
        butON.setVisibility(View.VISIBLE);
        butOFF.setVisibility(View.INVISIBLE);
        SharedPreferences BGSettings = getApplicationContext().getSharedPreferences("BackgroundService", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = BGSettings.edit();
        editor.putBoolean("Run", false);
        editor.apply();
        stopService(new Intent(this, BackgroundService.class));
    }
     */

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notifications";
            String description = "Get a notification when device is out of range.";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("BLENotificator" , name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createBGNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Background Service";
            String description = "Background Service";
            int importance = NotificationManager.IMPORTANCE_NONE;
            NotificationChannel channel = new NotificationChannel("BLENotificatorBackgroundService" , name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy(){
        if (butOFF.getVisibility() == View.VISIBLE){
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("restartservice");
            broadcastIntent.setClass(this, Restarter.class);
            this.sendBroadcast(broadcastIntent);
        }
        super.onDestroy();
    }







    //---------------------------------------------------------------------------------------------------------------------------------------------


/**

    public void testBackground(View v){
        Runnable newThread = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    testNotification();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        };
        newThread.run();
    }

    public void testNotification(){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder b = new NotificationCompat.Builder(getApplicationContext());

        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setTicker("Hearty365")
                .setContentTitle("Default notification")
                .setContentText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
                .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
                .setContentIntent(contentIntent)
                .setChannelId("BLENotificator")
                .setContentInfo("Info");


        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, b.build());
    }
    */
}