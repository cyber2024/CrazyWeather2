package com.ggg.crazyweather;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.ggg.crazyweather.fragments.FragmentDayDetail;
import com.ggg.crazyweather.fragments.FragmentMainActivity;
import com.ggg.crazyweather.helpers.Utility;
import com.ggg.crazyweather.sync.WeatherSyncAdapter;

public class MainActivity extends AppCompatActivity implements FragmentMainActivity.OnFragmentInteractionListener, FragmentDayDetail.OnFragmentInteractionListener{
    public static final String TAG = MainActivity.class.getSimpleName();
    private boolean mTwoPane;

    private String mLocation;

    @Override
    protected void onResume() {
        super.onResume();
        String newLocation = Utility.getPreferredLocation(this);
        if( newLocation != mLocation){
            mLocation = newLocation;
            FragmentManager fm = getSupportFragmentManager();
            FragmentMainActivity frag = (FragmentMainActivity) fm.findFragmentById(R.id.main_fragment
            );
            frag.onLocationChanged();

            FragmentDayDetail df = (FragmentDayDetail) fm.findFragmentByTag(FragmentDayDetail.TAG);
            if(df != null){
                df.onLocationChanged(mLocation);
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//
//        getSupportActionBar().setElevation(0);

        mLocation = Utility.getPreferredLocation(this);
        if (findViewById(R.id.detail_fragment_container) != null) {
            mTwoPane = true;
            if (savedInstanceState == null) {
                FragmentManager fm = getSupportFragmentManager();
                fm.beginTransaction()
                        .replace(R.id.detail_fragment_container, FragmentDayDetail.newInstance(Uri.parse(""), ""), FragmentDayDetail.TAG)
                        .commit();
                ((FragmentMainActivity) fm.findFragmentById(R.id.main_fragment)).setUseTodayLayout(!mTwoPane);
            }
        } else {
            mTwoPane = false;
            ((FragmentMainActivity) getSupportFragmentManager().findFragmentById(R.id.main_fragment)).setUseTodayLayout(!mTwoPane);
        }

        WeatherSyncAdapter.initializeSyncAdapter(this);
    }


    @Override
    public void selectedListItem(Uri weatherWithLocationAndDateUri) {
        if(mTwoPane){
            Log.d(TAG, "List item selected in twopane mode");
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction()
                    .replace(R.id.detail_fragment_container, FragmentDayDetail.newInstance(weatherWithLocationAndDateUri,""),FragmentDayDetail.TAG)
                    .addToBackStack("com.ggg.crazyweather.fragments.FragmentDayDetail")
                    .commit();
        } else {
            Intent intent = new Intent(this, DayDetailActivity.class)
                .setData(weatherWithLocationAndDateUri);
            startActivity(intent);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void startSettings() {
        Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

}
