package com.ggg.crazyweather.TestUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

import com.ggg.crazyweather.persistence.WeatherContract.LocationEntry;
import com.ggg.crazyweather.persistence.WeatherContract.WeatherEntry;
import com.ggg.crazyweather.persistence.WeatherDBHelper;

import java.util.Map;
import java.util.Set;

/**
 * Created by Russell Elfenbein on 3/9/2016.
 */
public class TestUtilities extends AndroidTestCase{

    public static final String TEST_LOCATION = "99705";
    public static final long TEST_DATE = 1419033600L; //Dec 20th, 2014


    public static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues){
        Set<Map.Entry<String,Object>> valueSet = expectedValues.valueSet();
        for(Map.Entry<String, Object> entry : valueSet){
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse("Column '" + columnName + "' not found." + error, idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals("Value '" + entry.getValue().toString() +
            "' did not match the expected value '" +
            expectedValue + "'." + error, expectedValue, valueCursor.getString(idx));
        }
    }

    public static void validateCursor(String error, Cursor valueCursor, ContentValues expectedValues){
        assertTrue("Empty cursor returned. " + error, valueCursor.moveToFirst());
        validateCurrentRecord(error, valueCursor, expectedValues);
        valueCursor.close();
    }

    public static ContentValues createWeatherValues(long locationRowId){
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationRowId);
        weatherValues.put(WeatherEntry.COLUMN_DATE, TEST_DATE);
        weatherValues.put(WeatherEntry.COLUMN_DEGREES, 1.1);
        weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, 1.2);
        weatherValues.put(WeatherEntry.COLUMN_PRESSURE, 1.3);
        weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, 75);
        weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, 65);
        weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
        weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, 5.5);
        weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, 321);

        return weatherValues;
    }

    public static ContentValues createNorthPoleValues(){
        ContentValues testValues = new ContentValues();

        testValues.put(LocationEntry.COLUMN_LOCATION_SETTING, TEST_LOCATION);
        testValues.put(LocationEntry.COLUMN_CITY_NAME, "North Pole");
        testValues.put(LocationEntry.COLUMN_COORD_LAT, 64.7488);
        testValues.put(LocationEntry.COLUMN_COORD_LONG, -147.353);

        return testValues;
    }

    public static long insertNorthPoleLocationValues(Context context){
        WeatherDBHelper dbHelper = new WeatherDBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues testValues = TestUtilities.createNorthPoleValues();

        long locationRowId;
        locationRowId = db.insert(LocationEntry.TABLE_NAME, null, testValues);

        assertTrue("Error: failure to insert north pole location values", locationRowId != -1);

        return locationRowId;
    }

    public static class TestContentObserver extends ContentObserver{
        final HandlerThread mHT;
        boolean mContentChanged;
        static TestContentObserver getTestContentObserver(){
            HandlerThread ht = new HandlerThread("ContentObserverThread");
            ht.start();
            return new TestContentObserver(ht);
        }

        private TestContentObserver(HandlerThread ht){
            super(new Handler(ht.getLooper()));
            mHT = ht;
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mContentChanged = true;
        }

        public void waitForNotificationOrFail(){
            new PollingCheck(5000){
                @Override
                protected boolean check(){
                    return mContentChanged;
                }
            }.run();
            mHT.quit();
        }
    }
    public static TestContentObserver getTestContentObserver(){
        return TestContentObserver.getTestContentObserver();
    }



}
