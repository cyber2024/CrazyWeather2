package com.ggg.crazyweather.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Russell Elfenbein on 3/14/2016.
 */
public class WeatherAuthenticatorService extends Service{
    private WeatherAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new WeatherAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
