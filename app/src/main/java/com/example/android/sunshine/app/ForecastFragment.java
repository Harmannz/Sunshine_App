/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = ForecastFragment.class.getSimpleName();
    // Loaders, in particular CursorLoader, are expected to retain their data after being stopped.
    // This allows applications to keep their data across the activity or fragment's onStop() and onStart() methods,
    // so that when users return to an application, they don't have to wait for the data to reload.
    private ForecastAdapter mForecastAdapter;
    private static final int FORECAST_LOADER = 0; // the loader id has to be unique for every loader you use in your activity

    private boolean mUseTodayLayout; // used to decide whether to display the todal layout normally or in larger view


    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.

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

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    private ListView mListView; //??
    private int mposition = ListView.INVALID_POSITION; //position fragment to hold the current position in the scroll
    private static final String SELECTED_KEY = "selected_position";

    public ForecastFragment() {
    }


    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
//        if (id == R.id.action_refresh) {
//            updateWeather();
//            return true;
//        }

        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap() {
        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        if (null != mForecastAdapter) {
            Cursor c = mForecastAdapter.getCursor();
            if (null != c) {
                c.moveToPosition(0);
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);


                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
                }

            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //The cursoradapter will take data from our cursor and populate the listview
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);


        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mForecastAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            /*
             * This method is invoked when item is clicked
             */
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null

                //if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    //we use callback instead of launching a new intent. MainActivity implements the callback interface
                    ((Callback) getActivity()).onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                            locationSetting, cursor.getLong(COL_WEATHER_DATE)
                    ));
                }

                mposition = position;//this stores the current position as a field so that when device is rotated,
                //we can scroll to that position
            }
        });
        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things. It should feel like some stuff streteched out,
        // or magically appeared to take advantage of room, but data or place in app was
        // never actually *lost*.
        /**
         * We use the onsavedINstanceState method to put the position as selectedkey value, so that
         * it can be retrieved here
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mposition = savedInstanceState.getInt(SELECTED_KEY);

        }
        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

        return rootView;
    }

    private void updateWeather() {
        /**
         * We are  going to be using the syncadapter instead
         */
        SunshineSyncAdapter.syncImmediately(getActivity());
        /**
         * Instead of using alarms and fetchweathertask, we are going to be using the sunshineservice
         */

//        Intent alarmIntent = new Intent(getActivity(), SunshineService.AlarmReceiver.class);
//        //put the location as StringExtra
//        alarmIntent.putExtra(SunshineService.LOCATION_QUERY_EXTRA, Utility.getPreferredLocation(getActivity()));
//
//        // Wrap in a pending intent which only fires once. We wake up a broadcast receiver
//        // and register intent filter.
//        // PendingIntent is intent that is handed to another application with same permissions. This allows the
//        // system application to call the application back in an asynchronous way.
//        PendingIntent pi = PendingIntent.getBroadcast(getActivity(), 0, alarmIntent, PendingIntent.FLAG_ONE_SHOT);//getBroadcast(context, 0, i, 0);
//        /**
//         * In alarms, pendingintent is used by the alarm manager to talk to the broadcast receiver
//         * we create.
//         */
//        AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
//
//        //Set the AlarmManager to wake up the system.
//        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pi);
//
//        /**
//         * Rather than starting the fetchweathertask background fetch data,
//         * we create intent and pass in the location via putExtra. and start the service.
//         */
////        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
////        String location = Utility.getPreferredLocation(getActivity());
////        weatherTask.execute(location);
//        Intent intent = new Intent(getActivity(), SunshineService.class);
//        intent.putExtra(SunshineService.LOCATION_QUERY_EXTRA,
//                Utility.getPreferredLocation(getActivity()));
//        getActivity().startService(intent);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        /**
         * When activity is created, we initialize the laoder
         * if loader is not already created before.
         * We create the laoder with the id and onCreateLoader gets called
         */
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        //initLoader takes three parameters:
//       1. A unique ID that identifies the loader. In this example, the ID is 0.
//       2. Optional arguments to supply to the loader at construction (null in this example).
//       3. A LoaderManager.LoaderCallbacks implementation, which the LoaderManager calls to report loader events. In this example, the local class implements the LoaderManager.LoaderCallbacks interface, so it passes a reference to itself, this.

        super.onActivityCreated(savedInstanceState);
    }


    /**
     * When the location is changed we have to update the weather query and restart the loader
     */
    public void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        //This is called when a new loader needs to be created. This
        // has only one loader, so we don't really care about the id.
        // First, pick up the base uri to use depending on whether we are currently filtering

        //now we create and return a cursorloader that will take care of
        //creating a cursor for the data being displayed
        //we are loading a forecast.
        // First, check if the location with this city name exists in the db
        String locationSetting = Utility.getPreferredLocation(getActivity());


        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, filter the query to return weather only for
        // dates after or including today.
        //Sort order: Ascending,, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis()
        );

        Log.d("ForecastFragment.java", "Returning new cursor loader");
        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
//        uri — The URI for the content to retrieve.
//        projection — A list of which columns to return. Passing null will return all columns, which is inefficient.
//        selection — A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE itself). Passing null will return all rows for the given URI.
//        selectionArgs — You may include ?s in the selection, which will be replaced by the values from selectionArgs, in the order that they appear in the selection. The values will be bound as Strings.
//        sortOrder — How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort order, which may be unordered.
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        //once a loader has been loaded we swap the cursor in cursor adapter with cursor we created
        //(The framework will take care of closing the
        // old cursor once we return.)

        mForecastAdapter.swapCursor(cursor);

        if (mposition != ListView.INVALID_POSITION) {
            // if we don't need to restart the loader, and there's a desired position to restore to,
            // then we do so now
            mListView.smoothScrollToPosition(mposition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mForecastAdapter.swapCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to invalid_position.
        // So check for that before storing
        if (mposition != ListView.INVALID_POSITION) {
            //we now have the position stored as value for selected_key key
            outState.putInt(SELECTED_KEY, mposition);
        }
        super.onSaveInstanceState(outState);
    }


    /**
     * sets value of field and forecastadapter
     *
     * @param useTodayLayout
     */
    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null) {
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }
}
