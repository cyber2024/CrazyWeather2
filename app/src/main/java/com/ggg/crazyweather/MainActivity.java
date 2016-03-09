package com.ggg.crazyweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.ggg.crazyweather.fragments.FragmentMainActivity;
import com.ggg.crazyweather.server.ServerSync;

public class MainActivity extends AppCompatActivity implements FragmentMainActivity.OnFragmentInteractionListener, ServerSync.ServerCallback{
    public static final String TAG = MainActivity.class.getSimpleName();



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.container, FragmentMainActivity.newInstance("This fragment", ""), FragmentMainActivity.TAG)
                .commit();
        updateDataFromServer();
    }

    @Override
    public void selectedListItem(String data) {
        Intent intent = new Intent(this, DayDetailActivity.class);
        intent.putExtra("extradata", data);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateDataFromServer();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void updateDataFromServer() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        ServerSync.fetchWeatherData(MainActivity.this, ServerSync.Method.GET, ServerSync.Endpoint.WEEK_FORECAST, location);
    }

    @Override
    public void startSettings() {
        Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onServerResponse(final String[] responseJson) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentMainActivity frag = (FragmentMainActivity) fm.findFragmentByTag(FragmentMainActivity.TAG);
        frag.updateData(responseJson);
    }
}
