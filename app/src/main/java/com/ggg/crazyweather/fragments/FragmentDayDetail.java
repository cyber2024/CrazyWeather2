package com.ggg.crazyweather.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ggg.crazyweather.R;
import com.ggg.crazyweather.helpers.Utility;
import com.ggg.crazyweather.persistence.ForecastAdapter;
import com.ggg.crazyweather.persistence.WeatherContract;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentDayDetail.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentDayDetail#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentDayDetail extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    public static final int LOADER_ID = 0;
    private ForecastAdapter mForecastAdapter;
    private String mForecastString;

    private ShareActionProvider mShareActionProvider;

    private TextView
            mTvDay,
            mTvDate,
            mTvDesc,
            mTvMax,
            mTvMin,
            mTvHumidity,
            mTvPressure,
            mTvWind;
    private ImageView mIvWeatherIcon;

    public static final String[] COLUMN_PROJECTION = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

    public static final int
            COL_WEATHER_ID = 0,
            COL_WEATHER_DATE = 1,
            COL_WEATHER_DESC = 2,
            COL_WEATHER_MAX_TEMP = 3,
            COL_WEATHER_MIN_TEMP = 4,
            COL_HUMIDITY = 5,
            COL_PRESSURE = 6,
            COL_WIND_SPEED = 7,
            COL_WIND_DIRECTION = 8,
            COL_WEATHER_ICON_ID = 9;

    public static final String TAG = FragmentDayDetail.class.getSimpleName();
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String DETAILS = "param1";
    private static final String ARG_PARAM2 = "param2";

    private final String SHARE_TAG = " #CrazyWeatherApp";

    // TODO: Rename and change types of parameters
    private Uri mWeatherWithLocationAndDateUri;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public FragmentDayDetail() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(0, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detail_fragment, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        if(mForecastString != null)
            mShareActionProvider.setShareIntent(createShareIntent());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_share:
            startActivity(createShareIntent());
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
 //    * @param mWeatherWithLocationAndDateUri Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentDayDetail.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentDayDetail newInstance(Uri weatherWithLocationAndDateUri, String param2) {
        FragmentDayDetail fragment = new FragmentDayDetail();
        Bundle args = new Bundle();
        args.putString(DETAILS, weatherWithLocationAndDateUri.toString());
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mWeatherWithLocationAndDateUri = Uri.parse(getArguments().getString(DETAILS));
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mTvDay = (TextView) rootView.findViewById(R.id.tvListItemDay);
        mTvDate = (TextView) rootView.findViewById(R.id.tvListItemDate);
        mTvDesc = (TextView) rootView.findViewById(R.id.tvListItemWeatherDesc);
        mTvMax = (TextView) rootView.findViewById(R.id.tvListItemMax);
        mTvMin = (TextView) rootView.findViewById(R.id.tvListItemMin);
        mTvHumidity = (TextView) rootView.findViewById(R.id.tvListItemHumidity);
        mTvPressure = (TextView) rootView.findViewById(R.id.tvListItemPressure);
        mTvWind = (TextView) rootView.findViewById(R.id.tvListItemWind);

        mIvWeatherIcon = (ImageView) rootView.findViewById(R.id.list_item_icon);

        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private Intent createShareIntent(){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastString + SHARE_TAG);
        return shareIntent;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(TAG, "In onCreateLoader");
        return new CursorLoader(
                getActivity(),
                mWeatherWithLocationAndDateUri,
                COLUMN_PROJECTION,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(TAG, "In onLoadFinished");
        if(data == null || !data.moveToFirst()) {
            Log.e(TAG, "onLoadFinished started with a null cursor");
            return;
        }

        int weatherId = data.getInt(COL_WEATHER_ICON_ID);
        mIvWeatherIcon.setImageResource(Utility.getWeatherArtResourceId(weatherId));

        long date = data.getLong(COL_WEATHER_DATE);
        String friendlyDateText = Utility.getDayName(getActivity(), date);
        String dateText = Utility.getFormattedMonthDay(getActivity(), date);
        mTvDay.setText(friendlyDateText);
        mTvDate.setText(dateText);

        String weatherDescription = data.getString(COL_WEATHER_DESC);
        mTvDesc.setText(weatherDescription);

        boolean isMetric = Utility.isMetric(getActivity());

        String max = Utility.formatTemperature(getActivity(),
                data.getDouble(COL_WEATHER_MAX_TEMP));
        mTvMax.setText(max);
        String min = Utility.formatTemperature(getActivity(),
                data.getDouble(COL_WEATHER_MIN_TEMP));
        mTvMin.setText(min);
        float pressure = data.getFloat(COL_PRESSURE);
        mTvPressure.setText(getActivity().getString(R.string.format_pressure, pressure));
        float humidity = data.getFloat(COL_HUMIDITY);
        mTvHumidity.setText(getActivity().getString(R.string.format_humidity, humidity));
        float windSpeed = data.getFloat(COL_WIND_SPEED);
        float windDir = data.getFloat(COL_WIND_DIRECTION);
        mTvWind.setText(Utility.getFormattedWind(getActivity(), windSpeed, windDir));

        String high = Utility.formatTemperature(
                getActivity(),
                data.getDouble(COL_WEATHER_MAX_TEMP)
        );
        String low = Utility.formatTemperature(
                getActivity(),
                data.getDouble(COL_WEATHER_MIN_TEMP)
        );

        mForecastString = String.format("%s - %s - %s/%s", dateText, weatherDescription, high, low);


        if(mShareActionProvider != null){
            mShareActionProvider.setShareIntent(createShareIntent());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.v(TAG, "In onLoaderReset");

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
        void startSettings();
    }

    public void onLocationChanged(String location){
        Uri uri = mWeatherWithLocationAndDateUri;
        if(uri != null){
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri newUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    location, date);
            mWeatherWithLocationAndDateUri = newUri;
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }

}
