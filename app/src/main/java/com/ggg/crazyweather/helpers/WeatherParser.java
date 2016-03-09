package com.ggg.crazyweather.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;

import com.ggg.crazyweather.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;

/**
 * Created by Russell Elfenbein on 3/9/2016.
 */
public class WeatherParser {

    public static String getReadableDateString(long time){
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    public static String formatHighLows(Context context, double high, double low){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String units = prefs.getString(context.getString(R.string.pref_units_key), context.getString(R.string.pref_units_default));

        if(!units.equals(context.getString(R.string.pref_units_default))){
            high = (high*1.8)+32;
            low = (low*1.8)+32;

            return Math.round(high) + "°F/" + Math.round(low)+"°F";
        }
        return Math.round(high) + "°C/" + Math.round(low)+"°C";
    }

    public static String[] getWeatherDataFromJson(Context context, String forecastJsonString, int numDays){
        final String OWM_LIST = "list",
                OWM_WEATHER = "weather",
                OWM_TEMPERATURE = "main",
                OWM_MAX = "temp_max",
                OWM_MIN = "temp_min",
                OWM_DESCRIPTION = "description";
        String[] resultString;
        try {
            JSONObject forecastJson = new JSONObject(forecastJsonString);
            JSONArray weatherJSONArray = forecastJson.getJSONArray(OWM_LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            dayTime = new Time();

            resultString = new String[numDays];
            for (int i = 0; i < weatherJSONArray.length(); i++){
                String day;
                String description;
                String highAndLow;

                JSONObject dayForecast = weatherJSONArray.getJSONObject(i);

                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                highAndLow = formatHighLows(context,
                        temperatureObject.getDouble(OWM_MAX),
                        temperatureObject.getDouble(OWM_MIN)
                );
                resultString[i] = day + " - " + description + " - " + highAndLow;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            resultString = new String[] {"Error"};
        }
        return resultString;
    }

}
