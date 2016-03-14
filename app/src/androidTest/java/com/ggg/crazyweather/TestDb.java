package com.ggg.crazyweather;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.ApplicationTestCase;
import android.util.Log;

import com.ggg.crazyweather.TestUtils.TestUtilities;
import com.ggg.crazyweather.persistence.WeatherContract;
import com.ggg.crazyweather.persistence.WeatherContract.LocationEntry;
import com.ggg.crazyweather.persistence.WeatherContract.WeatherEntry;
import com.ggg.crazyweather.persistence.WeatherDBHelper;

import java.sql.SQLException;
import java.util.HashSet;

/**
 * Created by Russell Elfenbein on 3/9/2016.
 */
public class TestDb extends ApplicationTestCase<Application> {
    public TestDb() {
        super(Application.class);
    }

    public static final String TAG = TestDb.class.getSimpleName();

    void deleteTheDb(){
        mContext.deleteDatabase(WeatherDBHelper.DATABASE_NAME);
    }

    public void setUp(){
        deleteTheDb();
    }

    public void testCreateDb() throws Throwable{
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(WeatherEntry.TABLE_NAME);
        tableNameHashSet.add(WeatherContract.LocationEntry.TABLE_NAME);

        mContext.deleteDatabase(WeatherDBHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDBHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'",null);
        assertTrue("Error: This means that the database has not been created properly", c.moveToFirst());

        do {
            tableNameHashSet.remove(c.getString(0));
        } while(c.moveToNext());

        assertTrue("Error: your table was created without both of the required tables", tableNameHashSet.isEmpty());

        c = db.rawQuery("PRAGMA table_info(" + LocationEntry.TABLE_NAME + ")", null);

        assertTrue("Error: this means that we were unable to query the database for the table information.",
                c.moveToFirst());

        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(LocationEntry.COLUMN_CITY_NAME);
        locationColumnHashSet.add(LocationEntry.COLUMN_COORD_LAT);
        locationColumnHashSet.add(LocationEntry.COLUMN_COORD_LONG);
        locationColumnHashSet.add(LocationEntry.COLUMN_LOCATION_SETTING);
        locationColumnHashSet.add(LocationEntry._ID);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        }while (c.moveToNext());

        assertTrue("Error: The database doesnt contain all of the required location entry columns",
                locationColumnHashSet.isEmpty());
        db.close();

    }

    public void testLocationTable(){
        try {
            insertLocation();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void testWeatherTable(){
        long locationRowId = 0;
        try {
            locationRowId = insertLocation();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        assertFalse("ErrorL Location not inserted properly", locationRowId == -1L);

        WeatherDBHelper dbHelper = new WeatherDBHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues weatherValues = TestUtilities.createWeatherValues(locationRowId);

        long weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues);
        assertTrue(weatherRowId != -1);

        Cursor c = db.query(
                WeatherEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue("Error: no record returned from lcoation query", c.moveToFirst());

        TestUtilities.validateCurrentRecord("error: Location Validation failed",
                c, weatherValues);

        assertFalse("Error: more than one record returned from location query",
                c.moveToNext());
        c.close();
        db.close();
    }

    public long insertLocation() throws SQLException{
        WeatherDBHelper dbHelper = new WeatherDBHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createNorthPoleValues();

        long locationRowId;
        locationRowId = db.insert(LocationEntry.TABLE_NAME, null, testValues);
        Log.d(TAG, "Location row = " + locationRowId);

        assertTrue(locationRowId != -1L);

        Cursor c = db.query(
                LocationEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue("Error: no records returned from location query", c.moveToFirst());

        TestUtilities.validateCurrentRecord("Error: Location query validation failed",
                c, testValues);

        assertFalse("Error: More than one record returned from lcoation query",c.moveToNext());
        c.close();
        db.close();
        return locationRowId;
    }

}
