package com.arnovaneetvelde.blenotificator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class Restarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences BGSettings = context.getSharedPreferences("BackgroundService", Context.MODE_PRIVATE);
        if (BGSettings.getBoolean("Run", true)) {
            Log.i("Broadcast Listened", "Service tried to stop");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, BackgroundService.class));
            } else {
                context.startService(new Intent(context, BackgroundService.class));
            }
        }
    }
}