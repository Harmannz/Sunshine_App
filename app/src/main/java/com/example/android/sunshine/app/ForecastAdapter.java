package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {

    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    private boolean mUseTodayLayout; // used to decide whether to display the todal layout normally or in larger view

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        boolean isMetric = Utility.isMetric(mContext);
        String highLowStr = Utility.formatTemperature(mContext, high, isMetric) + "/" + Utility.formatTemperature(mContext, low, isMetric);
        return highLowStr;
    }

    /*
        This is ported from FetchWeatherTask --- but now we go straight from the cursor to the
        string.
        THIS TAKES A ROW FROM THE CURSOR AND CONSTRUCTS A SINGLE STRING OF THE FORMAT:
            Date - Weather -- High/Low.
        It uses formatHighLow to get the correct string for the temperature.
     */
    private String convertCursorRowToUXFormat(Cursor cursor) {
        // WE dont need these constants as we have a projection in the cursor so we simply get from cursor
//        int idx_max_temp = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP);
//        int idx_min_temp = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP);
//        int idx_date = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
//        int idx_short_desc = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC);

        String highAndLow = formatHighLows(
                cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP),
                cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));

        return Utility.formatDate(cursor.getLong(ForecastFragment.COL_WEATHER_DATE)) +
                " - " + cursor.getString(ForecastFragment.COL_WEATHER_DESC) +
                " - " + highAndLow;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return 2; //this is the number of views that the cursor adapter will allow
    }

    /*
        Remember that adapters work with listviews to populate them. They create duplicates
        of the same layout to put into the list view. This is where you return what layout is
        going to be duplicated.
        Remember that these views are reused as needed.
        //New view takes an empty view
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose the layout type
        int viewType = getItemViewType(cursor.getPosition()); //this helps us determine which view to inflate based on cursor pos.
        int layoutId = -1; //defualt initialization

        if (viewType == VIEW_TYPE_TODAY) {
            layoutId = R.layout.list_item_forecast_today;
        } else {
            //default to 'normal' forecast view
            layoutId = R.layout.list_item_forecast;
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        //WE are using viewHolder to avoid repeated calls for findViewById for recycled views
        ViewHolder viewHolder = new ViewHolder(view); //
        view.setTag(viewHolder); //The tag of a view can be used to store any object. Then in bindView, we will get viewholder via tag of view
        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        /**
         * Because we are using viewHolder, we dont have to findviewbyid anymore.
         * We simply get the view we want to edit
         */
        //Read weather icon ID from cursor
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        /**
         * We use the weatherID to retrieve the weather condition for drawing appropriate image
         * Default image example --> <viewHolder.iconView.setImageResource(R.drawable.ic_launcher);
         */
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        /**
         * Here we can get what view type we are currently drawing
         * if the cursor position is 1st then we draw the larger image,
         * else we draw the grey image
         */
        int viewType = getItemViewType(cursor.getPosition());
        switch (viewType) {
            case VIEW_TYPE_TODAY: {
                viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
                break;
            }
            case VIEW_TYPE_FUTURE_DAY: {
                viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherId));
            }
        }
        // Read date from cursor
        long dateInMillis = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        // Find tetview and set formatted date on it
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateInMillis));

        // Read weather forecast from cursor
        String description = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        viewHolder.descriptionView.setText(description);

        boolean isMetric = Utility.isMetric(context);

        //Read high and low temperature from cursor
        double high = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        String highInString = Utility.formatTemperature(context, high, isMetric);
        viewHolder.highTempView.setText(highInString);

        double low = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        String lowInString = Utility.formatTemperature(context, low, isMetric);
        viewHolder.lowTempView.setText(lowInString);


    }

    /**
     * Cache of children views for a forecast list item
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView, descriptionView, highTempView, lowTempView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
        }
    }
}
