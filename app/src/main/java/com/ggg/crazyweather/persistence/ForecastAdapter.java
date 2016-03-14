package com.ggg.crazyweather.persistence;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ggg.crazyweather.R;
import com.ggg.crazyweather.fragments.FragmentMainActivity;
import com.ggg.crazyweather.helpers.Utility;

/**
 * Created by Russell Elfenbein on 3/12/2016.
 */
public class ForecastAdapter extends CursorAdapter {

    public static final int
            VIEW_TYPE_TODAY = 0,
            VIEW_TYPE_FUTURE_DAY = 1;

    private boolean mUseTodayLayout;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    private String formatHighLows(double high, double low){
        boolean isMetric = Utility.isMetric(mContext);
        String highLowStr = Utility.formatTemperature(mContext ,high)+"/"+Utility.formatTemperature(mContext, low);
        return highLowStr;
    }

    private String convertCursorRowToUXFormat(Cursor cursor){

        String highAndLow = formatHighLows(
                cursor.getDouble(FragmentMainActivity.COL_WEATHER_MAX_TEMP),
                cursor.getDouble(FragmentMainActivity.COL_WEATHER_MIN_TEMP)
        );
        return Utility.formatDate(cursor.getLong(FragmentMainActivity.COL_WEATHER_DATE)) +
                " - " + cursor.getString(FragmentMainActivity.COL_WEATHER_DESC) +
                " - " + highAndLow;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int layoutId;
        switch(getItemViewType(cursor.getPosition())){
            case VIEW_TYPE_TODAY:
                layoutId = R.layout.day_list_item_forecast_today;
                break;
            case VIEW_TYPE_FUTURE_DAY:
                layoutId = R.layout.day_list_item;
                break;
            default:
                layoutId = -1;
                break;
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        int weatherId = cursor.getInt(FragmentMainActivity.COL_WEATHER_CONDITION_ID);
        int viewType = getItemViewType(cursor.getPosition());

        switch(viewType){
            case VIEW_TYPE_TODAY:
                viewHolder.ivWeatherIcon.setImageResource(Utility.getWeatherArtResourceId(weatherId));
                break;
            case VIEW_TYPE_FUTURE_DAY:
                viewHolder.ivWeatherIcon.setImageResource(Utility.getWeatherIconResourceId(weatherId));
                break;
        }


        viewHolder.tvDate.setText(
                Utility.getFriendlyDate(context, cursor.getLong(FragmentMainActivity.COL_WEATHER_DATE))
        );
        viewHolder.tvDesc.setText(
                cursor.getString(FragmentMainActivity.COL_WEATHER_DESC)
        );
        viewHolder.tvMax.setText(
                Utility.formatTemperature(
                        context,
                        cursor.getDouble(FragmentMainActivity.COL_WEATHER_MAX_TEMP))
        );
        viewHolder.tvMin.setText(
                Utility.formatTemperature(
                        context,
                        cursor.getDouble(FragmentMainActivity.COL_WEATHER_MIN_TEMP))
        );

    }

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
    }


    public static class ViewHolder{
        public final ImageView ivWeatherIcon;
        public final TextView tvDate, tvDesc, tvMin, tvMax;
        public ViewHolder(View view){
            ivWeatherIcon = (ImageView) view.findViewById(R.id.list_item_icon);
            tvDate = (TextView) view.findViewById(R.id.tvListItemDate);
            tvDesc = (TextView) view.findViewById(R.id.tvListItemWeatherDesc);
            tvMin = (TextView) view.findViewById(R.id.tvListItemMin);
            tvMax = (TextView) view.findViewById(R.id.tvListItemMax);
        }
    }
}
