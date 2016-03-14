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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ggg.crazyweather.R;
import com.ggg.crazyweather.helpers.Utility;
import com.ggg.crazyweather.persistence.ForecastAdapter;
import com.ggg.crazyweather.persistence.WeatherContract;
import com.ggg.crazyweather.sync.WeatherSyncAdapter;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentMainActivity.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentMainActivity#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentMainActivity extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String TAG = FragmentMainActivity.class.getSimpleName();
    public static final int LOADER_ID = 0;
    int mPosition = 0;
    ListView mListView;
    private boolean mUseTodayLayout;

    public static final String[] FORECAST_COLUMN_PROJECTION = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };
    public static final int
            COL_WEATHER_ID = 0,
            COL_WEATHER_DATE = 1,
            COL_WEATHER_DESC = 2,
            COL_WEATHER_MAX_TEMP = 3,
            COL_WEATHER_MIN_TEMP = 4,
            COL_LOCATION_SETTING = 5,
            COL_WEATHER_CONDITION_ID = 6,
            COL_COORD_LAT = 7,
            COL_COORD_LONG = 8;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(0, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private ForecastAdapter mForecastAdapter;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public FragmentMainActivity() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentMainActivity.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentMainActivity newInstance(String param1, String param2) {
        FragmentMainActivity fragment = new FragmentMainActivity();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                updateWeather();
                return true;
            case R.id.action_settings:
                mListener.startSettings();
                return true;
            case R.id.action_show_on_map:
                openPreferredLocationOnMap();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(savedInstanceState != null){
            mPosition = savedInstanceState.getInt("position");
            Log.d(TAG, String.format("Loaded position %d", mPosition));
        }
        View rootView = inflater.inflate(R.layout.fragment_main_activity, container, false);
        mListView = (ListView) rootView.findViewById(R.id.lv_forecast_daily);

        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        mListView.setAdapter(mForecastAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                String locationSetting = Utility.getPreferredLocation(getActivity());
                mListener.selectedListItem(
                        WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                locationSetting,
                                cursor.getLong(COL_WEATHER_DATE)));
                mPosition = position;
            }
        });
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri weatherWithLocationUri =
                WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                        Utility.getPreferredLocation(getActivity()),
                        System.currentTimeMillis()
                );
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        return new CursorLoader(
                getActivity(),
                weatherWithLocationUri,
                FORECAST_COLUMN_PROJECTION,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
        mListView.setSelection(mPosition);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mPosition != ListView.INVALID_POSITION) {
            outState.putInt("position", mPosition);
            Log.d(TAG, String.format("Saved position %d", mPosition));
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
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
        void selectedListItem(Uri weatherWithLocationAndDateUri);

        void onFragmentInteraction(Uri uri);

        void startSettings();
    }

    public void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    public void openPreferredLocationOnMap() {

        if(mForecastAdapter != null){
            Cursor c = mForecastAdapter.getCursor();
            if(c != null){
                c.moveToPosition(0);
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);
                Intent intent = new Intent(Intent.ACTION_VIEW);

                intent.setData(geoLocation);
                if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(TAG, "Couldnt call " + geoLocation.toString() + ", no available app to show map");
                }
            }
        }
    }

    public void updateWeather() {
        WeatherSyncAdapter.syncImmediately(getActivity());

//        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
//        Intent intent = new Intent(getActivity(), WeatherService.AlarmReceiver.class);
//        intent.putExtra(WeatherService.LOCATION_QUERY_EXTRA,
//                Utility.getPreferredLocation(getActivity()));
//        PendingIntent alarmIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
//
//        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                SystemClock.elapsedRealtime() +5000, alarmIntent);
    }

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
        if(mForecastAdapter != null){
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }
}
