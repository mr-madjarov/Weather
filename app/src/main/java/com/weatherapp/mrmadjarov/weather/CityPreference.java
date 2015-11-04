package com.weatherapp.mrmadjarov.weather;

import android.app.Activity;
import android.content.SharedPreferences;

public class CityPreference {

    SharedPreferences prefs;

    public CityPreference(Activity activity){
        prefs = activity.getPreferences(Activity.MODE_PRIVATE);
    }

    // If the user has not chosen a city yet, return
    // Varna  as the default city
    public String getCity(){
        return prefs.getString("city", "Varna, BG");
    }

    void setCity(String city){
        prefs.edit().putString("city", city).commit();
    }

}

