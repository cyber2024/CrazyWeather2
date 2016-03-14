package com.ggg.crazyweather;

import android.net.Uri;
import android.test.AndroidTestCase;

import com.ggg.crazyweather.persistence.WeatherContract;

/**
 * Created by Russell Elfenbein on 3/10/2016.
 */
public class TestWeatherContract extends AndroidTestCase {
    private static final String TEST_WEATHER_LOCATION = "/North Pole";
    private static final long TEST_WEATHER_DATE = 1419033600L;

    public void testBuildWeatherLocation(){
        Uri locationUri = WeatherContract.WeatherEntry.buildWeatherWithLocationUri(TEST_WEATHER_LOCATION);
        assertNotNull("Error: Null Uri returned. You must fill in buildweatherlocation in Weather Contract", locationUri);
        assertEquals("Error: Weather location not properly appended to the end of the Uri",TEST_WEATHER_LOCATION, locationUri.getLastPathSegment());
        assertEquals("Error: weather location Uri doesnt match our expected result", locationUri.toString(), "content://com.ggg.crazyweather2/weather/%2FNorth%20Pole");
    }

}
