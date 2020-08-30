package com.arnovaneetvelde.blenotificator;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class BackgroundService extends Service {

    public Context context = this;
    public Handler handler = null;
    public static Runnable runnable = null;
    private final String TAG = "BG", NOTIFICATION_TAG = "BGNotification";
    private int counter = 0;
    private final int NOTIFICATION_ID = 121, maxSavedItems = 5;
    private NotificationManager notificationManager;
    private ArrayList<BLEDevice> savedDevices;
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner btScanner;

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Log.i(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        SharedPreferences BGSettings = getApplicationContext().getSharedPreferences("BackgroundService", Context.MODE_PRIVATE);
        if(!BGSettings.getBoolean("Run", true)) {
            handler.removeCallbacks(runnable);
            Log.i(TAG, "Run = false");
        } else {
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("restartservice");
            broadcastIntent.setClass(this, Restarter.class);
            this.sendBroadcast(broadcastIntent);
        }
        notificationManager.cancel(NOTIFICATION_TAG ,NOTIFICATION_ID);
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onStart(Intent intent, int startid) {
        Log.i(TAG, "onStart");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        savedDevices = new ArrayList<>();
        createBGNotification();
        getSavedDevices();

        counter = 0;
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                autoRun();
                Log.i(TAG, "run " + Integer.toString(counter));
                counter ++;
                handler.postDelayed(runnable, 60000);
            }
        };

        handler.postDelayed(runnable, 2000);

        return START_STICKY;
    }

    public void autoRun(){
        stopScanning();
        if (hasBlePermissions() && areLocationServicesEnabled(this)) {
            for (BLEDevice device : savedDevices) {
                if (!device.isInRange()) {
                    createNotification(device.getName());
                } else {
                    device.setRange(false);
                }
            }
            startScanning();
        } else {
            NotificationCompat.Builder b = new NotificationCompat.Builder(context);

            b.setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("No permission")
                    .setContentText("Please enable the required permissions.")
                    .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
                    .setChannelId("BLENotificator");

            notificationManager.notify(2, b.build());
        }
    }

    public void startScanning(){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning(){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            boolean allInRange = true;
            for (BLEDevice device : savedDevices){
                if (device.isEqual(result.getDevice().getAddress())){
                    device.setRange(true);
                    Log.i(TAG, "found device");
                }
                if (!device.isInRange()) allInRange = false;
            }
            if (allInRange) stopScanning();
        }
    };

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

    public void getSavedDevices(){
        SharedPreferences settings = getApplicationContext().getSharedPreferences("SavedDevices", Context.MODE_PRIVATE);
        savedDevices.clear();
        for (int i = 0; i < maxSavedItems; i++){
            if((settings.getString("Address" + Integer.toString(i), null) != null &&
                    !settings.getString("Address" + Integer.toString(i), null).equals("")) ||
                    (settings.getString("Name" + Integer.toString(i), null) != null &&
                            !settings.getString("Name" + Integer.toString(i), null).equals(""))) {
                BLEDevice device = new BLEDevice(settings.getString("Name" + i, null), settings.getString("Address" + i, null), 0, context);
                savedDevices.add(device);
            }
        }
    }

    public void createNotification(String text){
        NotificationCompat.Builder b = new NotificationCompat.Builder(context);

        b.setAutoCancel(false)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(text + " is out of range.")
                .setContentText("Don't forget your " + text)
                .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
                .setChannelId("BLENotificator");

        notificationManager.notify(1, b.build());
    }

    public void createBGNotification(){
        NotificationCompat.Builder b = new NotificationCompat.Builder(context);

        b.setAutoCancel(true)
                .setOngoing(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("App is running in the background")
                .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
                .setChannelId("BLENotificatorBackgroundService");

        notificationManager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, b.build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}