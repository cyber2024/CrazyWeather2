package com.ggg.crazyweather.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

import com.ggg.crazyweather.MainActivity;
import com.ggg.crazyweather.R;
import com.ggg.crazyweather.helpers.Utility;
import com.ggg.crazyweather.persistence.WeatherContract;

/**
 * Created by Russell Elfenbein on 3/14/2016.
 */
public class TodayWidgetIntentService extends IntentService {

    public static final String[] FORECAST_COL_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP
    };
    public static final int
            INDEX_WEATHER_ICON_ID = 0,
            INDEX_SHORT_DESC = 1,
            INDEX_MAX_TEMP = 2;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     *
     */
    public TodayWidgetIntentService() {
        super("TodayWeatherIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                TodayWidgetProvider.class));

        String location = Utility.getPreferredLocation(this);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location, System.currentTimeMillis()
        );
        Cursor data = getContentResolver().query(weatherForLocationUri, FORECAST_COL_PROJECTION, null, null,
                WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");
        if(data == null)
            return;
        if(!data.moveToFirst()){
            data.close();
            return;
        }

        int weatherArtResourceId = Utility.getWeatherArtResourceId(data.getInt(INDEX_WEATHER_ICON_ID));
        String description = data.getString(INDEX_SHORT_DESC);
        String formattedMaxTemp = Utility.formatTemperature(this, data.getDouble(INDEX_MAX_TEMP));

        data.close();

        for(int appWidgetId : appWidgetIds){
            int layoutId = R.layout.widget_small;
            RemoteViews views = new RemoteViews(this.getPackageName(), layoutId);

            views.setImageViewResource(R.id.ic_widget_icon, weatherArtResourceId);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
                setRemoteContentDescription(views, description);
            }
            views.setTextViewText(R.id.tv_widget_high_temp, formattedMaxTemp);

            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);

        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, String description){
        views.setContentDescription(R.id.ic_widget_icon, description);
    }
}
