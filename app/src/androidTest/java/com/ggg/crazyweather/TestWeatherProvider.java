package com.ggg.crazyweather;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

import com.ggg.crazyweather.TestUtils.TestUtilities;
import com.ggg.crazyweather.persistence.WeatherContract;
import com.ggg.crazyweather.persistence.WeatherDBHelper;
import com.ggg.crazyweather.persistence.WeatherProvider;

/**
 * Created by Russell Elfenbein on 3/10/2016.
 */
public class TestWeatherProvider extends AndroidTestCase {
    public static final String TAG = TestWeatherProvider.class.getSimpleName();

    public void deleteAllRecordsFromProvider(){
        mContext.getContentResolver().delete(
                WeatherContract.WeatherEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                WeatherContract.LocationEntry.CONTENT_URI,
                null,
                null
        );
        Cursor cursor = mContext.getContentResolver().query(
                WeatherContract.WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Weater table during delete",0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error, records not deleted from Location table during delete", 0, cursor.getCount());
        cursor.close();
    }



//    public void deleteAllRecordsFromDb(){
//        WeatherDBHelper dbHelper = new WeatherDBHelper(mContext);
//        SQLiteDatabase db = dbHelper.getWritableDatabase();
//        db.delete(WeatherContract.WeatherEntry.TABLE_NAME, null, null);
//        db.delete(WeatherContract.LocationEntry.TABLE_NAME, null, null);
//        db.close();
//    }

    public void deleteAllRecords(){
        deleteAllRecordsFromProvider();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteAllRecords();
    }

    public void testProviderRegistry(){
        PackageManager pm = mContext.getPackageManager();

        ComponentName componentName = new ComponentName(mContext.getPackageName(),
                WeatherProvider.class.getName());
        try {
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);
            assertEquals("Error: weather provider registered with authority: " + providerInfo.authority + " instead of authority: "+ WeatherContract.CONTENT_AUTHORITY,
                    providerInfo.authority, WeatherContract.CONTENT_AUTHORITY);
        } catch (PackageManager.NameNotFoundException e) {
            assertTrue("Error: WeatherProbvider not registered at " + mContext.getPackageName(),
                    false);
            e.printStackTrace();
        }
    }

    public void testGetType(){
        String type = mContext.getContentResolver().getType(WeatherContract.WeatherEntry.CONTENT_URI);
        assertEquals("Error: the WeatherEntry CONTENTURI should return WeatherEntry.CONTENT_TYPE",
                WeatherContract.WeatherEntry.CONTENT_TYPE, type);
        String testLocation = "94074";
        type = mContext.getContentResolver().getType(
                WeatherContract.WeatherEntry.buildWeatherLocation(testLocation));
        assertEquals("Error: WeatherEntry CONTENT_URI with location should return WeatherEntry.CONTEN_TYPE",
                WeatherContract.WeatherEntry.CONTENT_TYPE, type);
        long testDate = 1419120000L;
        type = mContext.getContentResolver().getType(
                WeatherContract.WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));
        assertEquals("Error: WeatherEntry CONTENT_URI with Location and Date should return WeatherEntry.CONTENT_ITEM_TYPE",
                WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE, type);
        type = mContext.getContentResolver().getType(WeatherContract.LocationEntry.CONTENT_URI);
        assertEquals("Error: LocationEntry CONTENT_URI should return LocationEntry.CONTENT_TYPE",
                WeatherContract.LocationEntry.CONTENT_TYPE, type);

    }

    public void testBasicLocationQueries(){
    WeatherDBHelper dbHelper = new WeatherDBHelper(mContext);
    SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createNorthPoleValues();
        long locationRowId = TestUtilities.insertNorthPoleLocationValues(mContext);

        Cursor locationCursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        Log.d(TAG, DatabaseUtils.dumpCursorToString(locationCursor));
        Log.d(TAG, "Test");

        TestUtilities.validateCursor("testBasicLocationQuesries", locationCursor, testValues);
        if(Build.VERSION.SDK_INT >= 19){
            assertEquals("Error; Location Query did not properly set NotificationUri",
                    locationCursor.getNotificationUri(), WeatherContract.LocationEntry.CONTENT_URI);
        }
    }
    public void testBasicWeatherQuery(){
        WeatherDBHelper dbHelper = new WeatherDBHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues testValues = TestUtilities.createNorthPoleValues();
        long locationRowId = TestUtilities.insertNorthPoleLocationValues(mContext);

        ContentValues weatherValues = TestUtilities.createWeatherValues(locationRowId);

        long weatherRowId = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, weatherValues);
        assertTrue("Unable to Insert WeatherEntry into the Database", weatherRowId != -1);

        db.close();

        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherContract.WeatherEntry.CONTENT_URI,
                null, null, null, null
        );

        TestUtilities.validateCursor("testBasicWeatherQUery", weatherCursor, weatherValues);
    }
    public void testUpdateLocation(){
        ContentValues testValues = TestUtilities.createNorthPoleValues();

        Uri locationUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, testValues);

        //get the locationId code from the uri
        long locationRowId = ContentUris.parseId(locationUri);

        assertTrue(locationRowId != -1);
        Log.d(TAG, "New row id: " + locationRowId);

        ContentValues updatedValues = new ContentValues();
        updatedValues.put(WeatherContract.LocationEntry._ID, locationRowId);
        updatedValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, "Santas Village");

        Cursor locationCursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                null, null, null, null);


        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        locationCursor.registerContentObserver(tco);

        int count = mContext.getContentResolver().update(
                WeatherContract.LocationEntry.CONTENT_URI,
                updatedValues, WeatherContract.LocationEntry._ID + "= ?",
                new String[]{Long.toString(locationRowId)}
        );
        assertEquals(count,1);
        tco.waitForNotificationOrFail();
        locationCursor.unregisterContentObserver(tco);
        locationCursor.close();

        Cursor cursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                null,
                WeatherContract.LocationEntry._ID + " = " + locationRowId,
                null,
                null
        );

        TestUtilities.validateCursor("testUpdateLocation. Error validating location entry update.",
                cursor, updatedValues);
        cursor.close();
    }

    public void testInsertReadProvider(){
        ContentValues testValues = TestUtilities.createNorthPoleValues();

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(WeatherContract.LocationEntry.CONTENT_URI, true, tco);
        Uri locationUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, testValues);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);
        long locationRowId = ContentUris.parseId(locationUri);
        assertTrue(locationRowId != -1);

        Cursor cursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,
                null, null, null, null);

        TestUtilities.validateCursor("testInsertReadProvider", cursor, testValues);

        ContentValues weatherValues = TestUtilities.createWeatherValues(locationRowId);
        tco = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(WeatherContract.WeatherEntry.CONTENT_URI, true, tco);
        Uri weatherInsertUri = mContext.getContentResolver().insert(WeatherContract.WeatherEntry.CONTENT_URI, weatherValues);
        assertTrue(weatherInsertUri != null);

        tco.waitForNotificationOrFail();

        mContext.getContentResolver().unregisterContentObserver(tco);

        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherContract.WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        TestUtilities.validateCursor("testInsertReadProvider",weatherCursor, weatherValues);
        weatherValues.putAll(testValues);

        weatherCursor = mContext.getContentResolver().query(
                WeatherContract.WeatherEntry.buildWeatherLocation(TestUtilities.TEST_LOCATION),
                null, null, null, null
        );
        TestUtilities.validateCursor("tesInsertReadProvider", weatherCursor, weatherValues);
        weatherCursor = mContext.getContentResolver().query(
                WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(TestUtilities.TEST_LOCATION, TestUtilities.TEST_DATE),
                null, null, null, null
        );
        TestUtilities.validateCursor("testInsertReadProvider, error validating joined weather and location data with start date", weatherCursor, weatherValues);

        weatherCursor = mContext.getContentResolver().query(
                WeatherContract.WeatherEntry.buildWeatherLocationWithDate(TestUtilities.TEST_LOCATION, TestUtilities.TEST_DATE),
                null, null, null, null
        );
        TestUtilities.validateCursor("testInsertReadProvider, error validatin joined weather and location data for a specific date", weatherCursor, weatherValues);
    }

    public void testDeleteRecords(){
        testInsertReadProvider();

        TestUtilities.TestContentObserver locationObserver = TestUtilities.getTestContentObserver();

        mContext.getContentResolver().registerContentObserver(WeatherContract.LocationEntry.CONTENT_URI, true, locationObserver);

        TestUtilities.TestContentObserver weatherObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(WeatherContract.WeatherEntry.CONTENT_URI, true, weatherObserver);

        deleteAllRecordsFromProvider();

        locationObserver.waitForNotificationOrFail();
        weatherObserver.waitForNotificationOrFail();

        mContext.getContentResolver().unregisterContentObserver(locationObserver);
        mContext.getContentResolver().unregisterContentObserver(weatherObserver);
    }

    static private final int BULK_INSERT_RECORDS_TO_INSERT = 10;
    static ContentValues[] createBulkInsertWeatherValues(long locationRowId) {
        long currentTestDate = TestUtilities.TEST_DATE;
        long millisecondsInADay = 1000*60*60*24;
        ContentValues[] returnContentValues = new ContentValues[BULK_INSERT_RECORDS_TO_INSERT];

        for ( int i = 0; i < BULK_INSERT_RECORDS_TO_INSERT; i++, currentTestDate+= millisecondsInADay ) {
            ContentValues weatherValues = new ContentValues();
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationRowId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, currentTestDate);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, 1.1);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, 1.2 + 0.01 * (float) i);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, 1.3 - 0.01 * (float) i);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, 75 + i);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, 65 - i);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, 5.5 + 0.2 * (float) i);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, 321);
            returnContentValues[i] = weatherValues;
        }
        return returnContentValues;
    }

    public void testBulkInsert(){
        ContentValues testValues = TestUtilities.createNorthPoleValues();
        Uri locationUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, testValues);
        long locationRowId = ContentUris.parseId(locationUri);
        assertTrue(locationRowId != -1);

        Cursor cursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        TestUtilities.validateCursor("testBulkInsert. Error validating locationEntry", cursor, testValues);

        ContentValues[] bulkInsertContentValues = createBulkInsertWeatherValues(locationRowId);
        TestUtilities.TestContentObserver weatherObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(WeatherContract.WeatherEntry.CONTENT_URI, true, weatherObserver);

        int insertCount = mContext.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, bulkInsertContentValues);

        weatherObserver.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(weatherObserver);

        assertEquals(insertCount, BULK_INSERT_RECORDS_TO_INSERT);

        cursor = mContext.getContentResolver().query(
                WeatherContract.WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                WeatherContract.WeatherEntry.COLUMN_DATE + " ASC"
        );
        assertEquals(cursor.getCount(), BULK_INSERT_RECORDS_TO_INSERT);
        assertTrue(cursor.moveToFirst());
        for(int i = 0; i < BULK_INSERT_RECORDS_TO_INSERT; i++, cursor.moveToNext()){
            TestUtilities.validateCurrentRecord("testBulkInsert. Error validating WeatherEntry " + i,
                    cursor, bulkInsertContentValues[i]);
        }
        cursor.close();
    }

}
