package com.arnovaneetvelde.blenotificator;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class BackgroundService extends Service {

    public Context context = this;
    public Handler handler = null;
    public static Runnable runnable = null;
    private final String TAG = "BG";
    private int counter = 0;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        /* IF YOU WANT THIS SERVICE KILLED WITH THE APP THEN UNCOMMENT THE FOLLOWING LINE */
        SharedPreferences BGSettings = getApplicationContext().getSharedPreferences("BackgroundService", Context.MODE_PRIVATE);
        if(!BGSettings.getBoolean("Run", true)) {
            handler.removeCallbacks(runnable);
            Log.i(TAG, "Run = false");
        }
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onStart(Intent intent, int startid) {
        Log.i(TAG, "onStart");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");

        counter = 0;
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                Log.i(TAG, "run " + Integer.toString(counter));
                counter ++;
                handler.postDelayed(runnable, 60000);
            }
        };

        handler.postDelayed(runnable, 2000);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}