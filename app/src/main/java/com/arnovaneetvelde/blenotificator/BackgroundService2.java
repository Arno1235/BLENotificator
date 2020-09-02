package com.arnovaneetvelde.blenotificator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BackgroundService2 extends BroadcastReceiver {

    private final String TAG = "BG";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");
    }
}
