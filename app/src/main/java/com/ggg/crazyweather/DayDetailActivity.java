package com.ggg.crazyweather;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.ggg.crazyweather.fragments.FragmentDayDetail;

public class DayDetailActivity extends AppCompatActivity implements FragmentDayDetail.OnFragmentInteractionListener{
    public Uri mForecastUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        if(intent != null)
            mForecastUri = intent.getData();


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.detail_fragment_container, FragmentDayDetail.newInstance(mForecastUri, ""), FragmentDayDetail.TAG)
                .commit();


    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void startSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
