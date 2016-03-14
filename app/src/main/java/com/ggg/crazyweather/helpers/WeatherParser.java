package com.ggg.crazyweather.helpers;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;

import com.ggg.crazyweather.persistence.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Vector;

/**
 * Created by Russell Elfenbein on 3/9/2016.
 */
public class WeatherParser {
    public static final String TAG = WeatherParser.class.getSimpleName();

    public static String getReadableDateString(long time){
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }


}
