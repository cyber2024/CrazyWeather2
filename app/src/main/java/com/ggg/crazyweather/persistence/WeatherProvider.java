package com.ggg.crazyweather.persistence;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Russell Elfenbein on 3/10/2016.
 */
public class WeatherProvider extends ContentProvider {
    public static final String TAG = WeatherProvider.class.getSimpleName();

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private WeatherDBHelper mOpenHelper;

    static final int WEATHER = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int LOCATION = 300;

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;
    static {
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();

        //Set up the table join
        sWeatherByLocationSettingQueryBuilder.setTables(
                WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        WeatherContract.LocationEntry.TABLE_NAME +
                        " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
                        "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                        " = " + WeatherContract.LocationEntry.TABLE_NAME +
                        "." + WeatherContract.LocationEntry._ID);
    }

    //location.location_setting = ?
    private static final String sLocationSettingSelection = WeatherContract.LocationEntry.TABLE_NAME+
            "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING+ " = ?";

    //location.location_setting = ? and date >= ?
    private static final String sLocationSettingWithStartDateSelection = WeatherContract.LocationEntry.TABLE_NAME +
            "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
            WeatherContract.WeatherEntry.COLUMN_DATE + " >= ? ";

    //location.location_setting = ? AND date = ?
    private static final String sLocationSettingWithDaySelection = WeatherContract.LocationEntry.TABLE_NAME +
            "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
            WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ";


    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder){
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if(startDate == 0){
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};

        } else {
            selectionArgs = new String[]{locationSetting, Long.toString(startDate)};
            selection = sLocationSettingWithStartDateSelection;
        }
        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    public Cursor getWeatherByLocationSettingAndDate(Uri uri, String[] projection, String sortOrder){
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long date = WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sLocationSettingWithDaySelection,
                new String[]{locationSetting, Long.toString(date)},
                null,
                null,
                sortOrder
                );

    }


    public static UriMatcher buildUriMatcher(){
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);
        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match){
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            default:
                throw  new UnsupportedOperationException("Unknown Uri: "+uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        //determine the type of request
        Log.w(TAG, uri.toString());
        Cursor retCursor;
        switch(sUriMatcher.match(uri)){
            case WEATHER_WITH_LOCATION_AND_DATE:
                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            case WEATHER_WITH_LOCATION:
                retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            case WEATHER:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case LOCATION:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch(match){
            case WEATHER: {
                normalizeDate(values);
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case LOCATION:
                long _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);
                if(_id > 0)
                    returnUri = WeatherContract.LocationEntry.buildLocationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsDeleted;
        switch(sUriMatcher.match(uri)){
            case WEATHER:
                rowsDeleted = db.delete(WeatherContract.WeatherEntry.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = db.delete(WeatherContract.LocationEntry.TABLE_NAME,
                        selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri: "+uri);
        }
        if(rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            Log.d(TAG, String.format("%1$d rows deleted.", rowsDeleted));
        }
        return rowsDeleted;
    }

    private void normalizeDate(ContentValues values){
        if(values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)){
            long dateValue = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(dateValue));
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsUpdated;
        switch(sUriMatcher.match(uri)){
            case WEATHER:
                rowsUpdated = db.update(WeatherContract.WeatherEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case LOCATION:
                rowsUpdated = db.update(WeatherContract.LocationEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknow Uri: "+uri);
        }
        if(rowsUpdated != 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match){
            case WEATHER:
                db.beginTransaction();;
                int returnCount = 0;
                try{
                    for(ContentValues value : values){
                        normalizeDate(value);
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if(_id != -1){
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
