package com.ggg.crazyweather.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Russell Elfenbein on 3/14/2016.
 */
public class WeatherSyncService extends Service {
    public static final String TAG = WeatherSyncService.class.getSimpleName();
    private static final Object sSyncAdapterLock = new Object();
    private static WeatherSyncAdapter sWaetherSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate - WeatherSyncService");
        synchronized (sSyncAdapterLock){
            if(sWaetherSyncAdapter == null){
                sWaetherSyncAdapter = new WeatherSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sWaetherSyncAdapter.getSyncAdapterBinder();
    }
}
