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
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

/**
 * The control flow for getting data from OWM and syncing it with app.

 Your MainActivity is created and the sync adapter is initialized.
 During initialization, getSyncAccount is called.
 getSyncAccount will create a new account if no sunshine.example.com account exists. If this is the case, onAccountCreated will be called.
 onAccountCreated configures the periodic sync and calls for an immediate sync. At this point, Sunshine will sync with the Open Weather API either every 3 hours (if the build version is less than KitKat) or everyone 1 hour (if the build version is greater than or equal to KitKat)
 */
public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private String mLocation;
    private final String DETAILFRAGMENT_TAG = "DFTAG";
    //Fragment tag is a constant string we can use to tag a fragment within the fragment manager
    //so we can easily look it up later.
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLocation = Utility.getPreferredLocation(this); //we will get the preferred location stored
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (findViewById(R.id.weather_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.weather_detail_container, new ForecastFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
            /**
             * Note how in the two pane case we check if the saved instance state is null. Why? Well if we rotate the phone, the system saves the fragment state in the saved state bundle and is smart enough to restore this state.
             * Therefore, if the saved state bundle is not null, the system already has the fragment it needs and you shouldn’t go adding another one.
             */
        } else {
            mTwoPane = false;
            //In one pane mode we get rid of the shadow underneath the action bar.
            this.getSupportActionBar().setElevation(0f);
        }

        /**
         * set boolean field of forecastfragment to determine whether or not we should
         * show the first weather in larger format or not (not == we are viewing in tablet)
         */
        ForecastFragment forecastFragment = ((ForecastFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_forecast));
        forecastFragment.setUseTodayLayout(!mTwoPane);

        SunshineSyncAdapter.initializeSyncAdapter(this); //initialise the sync adapter
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        String location = Utility.getPreferredLocation(this);
        // update the location in our second pane using the fragment manager
        if (location != null && !location.equals(mLocation)) {
            ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            if (null != ff) {
                ff.onLocationChanged();
            }
            mLocation = location;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // ***** adding or replacing  ***** the detail fragment using a
            // fragment transaction.
            /**
             * We create a bundle and put the detail uri as key<--> value pair.
             * The is attached to the new DetailFragment
             */
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, contentUri);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);
            /**
             * Here we replace the detailfragment in two pane mode.
             */
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            //Otherwise we are in single pane mode, thus we fire a new intent to create the detail activity
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }
    }

}
