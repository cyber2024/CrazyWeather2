package com.ggg.crazyweather.persistence;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;


/**
 * Created by Russell Elfenbein on 3/9/2016.
 */
public class WeatherContract {

    //Content authority is the name for the entire content provider, this can be anything, but for convenience the package name is used.
    public static final String CONTENT_AUTHORITY = "com.ggg.crazyweather2";

    //A base uri includes the content authority AND the scheme
    public static Uri BASE_CONTENT_URI = Uri.parse("content://" +CONTENT_AUTHORITY);

    //possible paths that the content provider will allow and return data for
    public static final String PATH_WEATHER = "weather";
    public static final String PATH_LOCATION = "location";

    //dates must be normalised to work around the globe, here we use UTC
    public static long normalizeDate(long startDate){
        Time time = new Time();
        time.set(startDate);
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
        return time.setJulianDay(julianDay);
    }

    /**
     * Location Entry
     */
    public static final class LocationEntry implements BaseColumns{
        public static Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        public static final String
        TABLE_NAME = "location",
        COLUMN_LOCATION_SETTING = "location_setting",
        COLUMN_CITY_NAME = "city_name",
        COLUMN_COORD_LAT = "coord_lat",
        COLUMN_COORD_LONG = "coord_long";

        public static Uri buildLocationUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    /**
     * Weather Entry
     */
    public static final class WeatherEntry implements BaseColumns{
        public static Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEATHER).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;

        public static Uri buildWeatherWithLocationUri(String locationSetting){
            return CONTENT_URI.buildUpon().appendPath(locationSetting).build();
        }

        public static final String
        TABLE_NAME = "weather",
        COLUMN_LOC_KEY = "location_id",
        COLUMN_DATE = "date",
        COLUMN_WEATHER_ID = "weather_id",
        COLUMN_SHORT_DESC = "short_desc",
        COLUMN_MIN_TEMP = "min",
        COLUMN_MAX_TEMP = "max",
        COLUMN_HUMIDITY = "humidity",
        COLUMN_PRESSURE = "pressure",
        COLUMN_WIND_SPEED = "wind",
        COLUMN_DEGREES = "degrees";

        public static Uri buildWeatherLocation(String locationSetting){
            return CONTENT_URI.buildUpon().appendPath(locationSetting).build();
        }
        public static Uri buildWeatherLocationWithStartDate(String locationSetting, long startDate){
            long normalizedDate = normalizeDate(startDate);
            return CONTENT_URI.buildUpon().appendPath(locationSetting)
                    .appendQueryParameter(COLUMN_DATE, Long.toString(normalizedDate)).build();
        }

        public static Uri buildWeatherLocationWithDate(String locationSetting, long date){
            return CONTENT_URI.buildUpon().appendPath(locationSetting)
                    .appendPath(Long.toString(normalizeDate(date))).build();
        }
        public static String getLocationSettingFromUri(Uri uri){
            return uri.getPathSegments().get(1);
        }
        public static long getDateFromUri(Uri uri){
            return Long.parseLong(uri.getPathSegments().get(2));
        }
        public static long getStartDateFromUri(Uri uri){
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if(dateString != null && dateString.length() > 0){
                return Long.parseLong(dateString);
            } else {
                return 0;
            }
        }
        public static Uri buildWeatherUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
