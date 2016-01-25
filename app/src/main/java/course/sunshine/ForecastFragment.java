package course.sunshine;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    final String LOG_TAG = ForecastFragment.class.getSimpleName();

    ArrayAdapter<String> weatherForecastAdapter;

    @Override
    public void onStop() {
        super.onStop();
        Log.v(LOG_TAG,"STOP");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(LOG_TAG, "RESUME");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(LOG_TAG, "PAUSE");
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
        Log.v(LOG_TAG, "START");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "DESTROY");
    }

    public ForecastFragment() {
    }

    private void updateWeather(){
        String location = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default));
        String units = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.pref_units_key),getString(R.string.pref_units_default));
        String[] sett = {location,units};
        new FetchWeatherTask().execute(sett);
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_refresh){
            updateWeather();
            return true;
        }else
        if(item.getItemId() == R.id.activity_settings){
            Intent settingsIntent = new Intent(getActivity(),SettingsActivity.class);
            startActivity(settingsIntent);
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

         weatherForecastAdapter = new ArrayAdapter<String>(
                 getActivity(),//Context
                 R.layout.list_item_forecast,//Resource
                 R.id.listItemForecastTextView,//TextView
                 new ArrayList<String>());//Data


        ListView tV = (ListView) rootView.findViewById(R.id.listViewForecast);
        tV.setAdapter(weatherForecastAdapter);
        tV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(getActivity(),parent.getItemAtPosition(position).toString(),Toast.LENGTH_LONG).show();
                Intent detailedIntent = new Intent(getActivity(),DetailActivity.class).putExtra(Intent.EXTRA_TEXT,parent.getItemAtPosition(position).toString());
                startActivity(detailedIntent);
            }
        });
        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String,Void,String[]>{

        private  final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        private String city;
        private final String mode = "json";
        private final String units = "metric";
        private final String key = "e3d16685d1017112785aa7580b952f30";

        public final static int METRIC = 0;
        public final static int IMPERIAL = 1;


        @Override
        protected void onPostExecute(String[] strings) {
            weatherForecastAdapter.clear();
            for(String str:strings)
                weatherForecastAdapter.add(str);
        }

        @Override
        protected String[] doInBackground(String... params) {
            //http://api.openweathermap.org/data/2.5/forecast/daily?q=Oviedo,es&cnt=7&mode=json&units=metric&APPID=e3d16685d1017112785aa7580b952f30
            Uri.Builder builder = new Uri.Builder();
            this.city = params[0];
            builder.scheme("http")
                    .authority("api.openweathermap.org")
                    .appendPath("data")
                    .appendPath("2.5")
                    .appendPath("forecast")
                    .appendPath("daily")
                    .appendQueryParameter("q",city)
                    .appendQueryParameter("cnt", "7")
                    .appendQueryParameter("mode",mode)
                    .appendQueryParameter("units",units)
                    .appendQueryParameter("APPID",key);
            Log.v(LOG_TAG,"URL: "+builder.build().toString());
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                URL url = new URL(builder.build().toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
                WeatherDataParser dataParser =  new WeatherDataParser();
                String[] forecastFormatedData = dataParser.getWeatherDataFromJson(forecastJsonStr,7,Integer.parseInt(params[1]));
                return forecastFormatedData;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return null;
        }
    }
}

