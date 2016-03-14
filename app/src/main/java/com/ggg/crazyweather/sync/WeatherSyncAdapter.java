package com.ggg.crazyweather.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

import com.ggg.crazyweather.BuildConfig;
import com.ggg.crazyweather.MainActivity;
import com.ggg.crazyweather.R;
import com.ggg.crazyweather.helpers.Utility;
import com.ggg.crazyweather.persistence.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Created by Russell Elfenbein on 3/14/2016.
 */
public class WeatherSyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = WeatherSyncAdapter.class.getSimpleName();

    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    private static final long DAY_IN_MILLIS = 1000 * 3600 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 2024;

    public static final String ACTION_DATA_UPDATED = "com.ggg.crazyweather.ACTION_DATA_UPDATED";

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };

    private static final int
            INDEX_WEATHER_ID = 0,
            INDEX_MAX_TEMP = 1,
            INDEX_MIN_TEMP = 2,
            INDEX_SHORT_DESC = 3;

    public WeatherSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "startSyncing");
        String locationQuery = Utility.getPreferredLocation(getContext());

        final String QUERY_PARAM = "q",
                FORMAT_PARAM = "mode",
                UNITS_PARAM = "units",
                DAYS_PARAM = "cnt",
                FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?",
                FORMAT = "json",
                UNITS = "metric";
        final int NUMDAYS = 14;

        Uri uri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                .appendQueryParameter(QUERY_PARAM, locationQuery)
                .appendQueryParameter(FORMAT_PARAM, FORMAT)
                .appendQueryParameter(UNITS_PARAM, UNITS)
                .appendQueryParameter(DAYS_PARAM, Integer.toString(NUMDAYS))
                .appendQueryParameter("apikey", BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                .build();


        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String response = null;
        int responseCode = -1;

        try {
            URL url = new URL(uri.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            responseCode = connection.getResponseCode();

            InputStream is;
            try {
                is = connection.getInputStream();
            } catch (IOException e) {
                is = connection.getErrorStream();
                e.printStackTrace();
            }
            StringBuffer buffer = new StringBuffer();
            if (is == null) {
                //nothing
                return;
            }
            reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + '\n');
            }

            if (buffer.length() == 0) {

                return;
            }

            response = buffer.toString();
            getWeatherDataFromJson(response, locationQuery);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error requesting forecast from server", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
        }
        return;
    }

    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    public static Account getSyncAccount(Context context) {
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account newAccount = new Account(context.getString(R.string.app_name), context.getString(R.string.sync_account_type));
        if (accountManager.getPassword(newAccount) == null) {

            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    public static void configurePeriodSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SyncRequest request = new SyncRequest.Builder()
                    .syncPeriodic(syncInterval, flexTime)
                    .setSyncAdapter(account, authority)
                    .setExtras(new Bundle()).build();
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        WeatherSyncAdapter.configurePeriodSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }


    public void getWeatherDataFromJson(String forecastJsonString, String locationSetting) {
        Log.d(TAG, forecastJsonString);
        final String
                OWM_CITY = "city",
                OWM_CITY_NAME = "name",
                OWM_COORD = "coord",
                OWM_LAT = "lat",
                OWM_LON = "lon",

                OWM_LIST = "list",
                OWM_PRESSURE = "pressure",
                OWM_HUMIDITY = "humidity",
                OWM_WINDSPEED = "speed",
                OWM_WIND_DIRECTION = "deg",

                OWM_WEATHER = "weather",

                OWM_TEMPERATURE = "temp",
                OWM_MAX = "max",
                OWM_MIN = "min",
                OWM_WEATHER_ID = "id",
                OWM_DESCRIPTION = "description";
        try {
            JSONObject forecastJson = new JSONObject(forecastJsonString);
            JSONArray weatherJSONArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);
            JSONObject cityCoordJson = cityJson.getJSONObject(OWM_COORD);
            double cityLat = cityCoordJson.getDouble(OWM_LAT);
            double cityLon = cityCoordJson.getDouble(OWM_LON);

            long locationRowId = addLocation(locationSetting, cityName, cityLat, cityLon);
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherJSONArray.length());

            Time dayTime = new Time();
            dayTime.setToNow();
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            dayTime = new Time();

            for (int i = 0; i < weatherJSONArray.length(); i++) {
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;
                double high;
                double low;
                String description;
                int weatherId;

                JSONObject dayForecast = weatherJSONArray.getJSONObject(i);

                dateTime = dayTime.setJulianDay(julianStartDay + i);

                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                JSONObject temperatureJson = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureJson.getDouble(OWM_MAX);
                low = temperatureJson.getDouble(OWM_MIN);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);


                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationRowId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);


                cVVector.add(weatherValues);
            }
            int inserted = 0;

            if (cVVector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                inserted = getContext().getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);

                getContext().sendBroadcast(new Intent(ACTION_DATA_UPDATED));

                if (Utility.showNofitications(getContext()))
                    notifyWeather();

                updateWidgets();

                getContext().getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,
                        WeatherContract.WeatherEntry.COLUMN_DATE + "<= ?",
                        new String[]{Long.toString(dayTime.setJulianDay(julianStartDay - 1))});
            }
            Log.d(TAG, String.format("getWeatherFromJson complete. Inserted %1$d into database.", inserted));

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long locationId;
        Cursor cursor = getContext().getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null
        );

        if (cursor.moveToFirst()) {
            int locationIdIndex = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId = cursor.getLong(locationIdIndex);
            Log.d(TAG, "Location found in location table");
        } else {

            Log.d(TAG, "Adding location to location table");
            ContentValues contentValues = new ContentValues();
            contentValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            Uri insertedUri = getContext().getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    contentValues
            );
            locationId = ContentUris.parseId(insertedUri);
        }
        cursor.close();
        return locationId;
    }

    public void notifyWeather() {
        Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        long lastSync = prefs.getLong(lastNotificationKey, 0);

        if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
            String locationQuery = Utility.getPreferredLocation(context);
            Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

            Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

            if (cursor.moveToFirst()) {
                int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                double high = cursor.getDouble(INDEX_MAX_TEMP);
                double low = cursor.getDouble(INDEX_MIN_TEMP);
                String desc = cursor.getString(INDEX_SHORT_DESC);

                int iconId = Utility.getWeatherIconResourceId(weatherId);
                String title = context.getString(R.string.app_name);

                String contentText = String.format(context.getString(R.string.format_notification),
                        desc,
                        Utility.formatTemperature(context, high),
                        Utility.formatTemperature(context, low));

                //build notification here
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(iconId)
                        .setContentTitle(title)
                        .setContentText(contentText);
                Intent resultIntent = new Intent(context, MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                notificationBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotoficationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotoficationManager.notify(WEATHER_NOTIFICATION_ID, notificationBuilder.build());

                prefs.edit()
                        .putLong(lastNotificationKey, System.currentTimeMillis())
                        .commit();
            }
        }



    }
    public void updateWidgets(){
        Context context = getContext();
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                .setPackage(context.getPackageName());
        context.sendBroadcast(dataUpdatedIntent);

    }
}
