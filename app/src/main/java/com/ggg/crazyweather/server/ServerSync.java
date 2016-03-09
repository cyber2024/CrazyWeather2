package com.ggg.crazyweather.server;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.ggg.crazyweather.BuildConfig;
import com.ggg.crazyweather.R;
import com.ggg.crazyweather.helpers.WeatherParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Russell Elfenbein on 3/9/2016.
 */
public class ServerSync {
    public static final String TAG = ServerSync.class.getSimpleName();
    public enum Method {GET,POST};
    public enum Endpoint {WEEK_FORECAST}

    public static void fetchWeatherData(final Context context, final Method method, Endpoint endpoint, String... params){
        if(params == null){
            params = new String[]{"Perth, WA"};
        }

        new AsyncTask<String, Void, String>() {
            ServerCallback mCallback;

            @Override
            protected String doInBackground(String... params) {
                if(context instanceof ServerCallback) {
                    mCallback = (ServerCallback) context;
                } else {
                    throw new ClassCastException(context.getPackageName()+" must implement ServerCallbackinterface");
                }

                final String QUERY_PARAM = "q",
                        FORMAT_PARAM = "mode",
                        UNITS_PARAM = "units",
                        DAYS_PARAM = "cnt";



                Uri.Builder builder = new Uri.Builder();
                builder.scheme(context.getString(R.string.openweather_scheme))
                        .authority(context.getString(R.string.openweather_authority))
                        .appendPath(context.getString(R.string.openweather_endpoint))
                        .appendQueryParameter(QUERY_PARAM,params[0])
                        .appendQueryParameter(FORMAT_PARAM, "json")
                        .appendQueryParameter(UNITS_PARAM, "metric")
                        .appendQueryParameter(DAYS_PARAM,"7")
                        .appendQueryParameter("apikey",BuildConfig.OPEN_WEATHER_MAP_API_KEY);


                HttpURLConnection connection = null;
                String urlString = builder.toString();
                BufferedReader reader = null;
                String response = null;
                int responseCode = -1;

                try {
                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    switch(method){
                        case GET:
                            connection.setRequestMethod("GET");
                            break;
                    }
                    connection.connect();
                    responseCode = connection.getResponseCode();
                    InputStream is;
                    try {
                        is = connection.getInputStream();
                    } catch (IOException e){
                        is = connection.getErrorStream();
                        e.printStackTrace();
                    }
                    StringBuffer buffer = new StringBuffer();
                    if(is == null){
                        //nothing
                        return null;
                    }
                    reader =  new BufferedReader(new InputStreamReader(is));

                    String line;
                    while((line = reader.readLine())!= null){
                        buffer.append(line + '\n');
                    }

                    if(buffer.length() == 0){

                        return null;
                    }

                    response = buffer.toString();

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error requesting forecast from server",e);
                } finally {
                    if(connection != null){
                        connection.disconnect();
                    }
                    if(reader != null){
                        try {
                            reader.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error closing stream",e);
                        }
                    }
                }
                return response;
            }

            @Override
            protected void onPostExecute(String response) {
                mCallback.onServerResponse(WeatherParser.getWeatherDataFromJson(context, response, 7));
            }
        }. execute(params);

    }

    public interface ServerCallback{
        public void onServerResponse(String[] response);
    }
}
