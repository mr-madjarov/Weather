package com.weatherapp.mrmadjarov.weather;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by mrmadjarov on 6/14/2015.
 */
public class FetchAddressIntentService extends IntentService {
    private static final String TAG = FetchAddressIntentService.class.getSimpleName();
    private static final AndroidHttpClient ANDROID_HTTP_CLIENT = AndroidHttpClient.newInstance(TAG);
    public ResultReceiver mReceiver;

    //Ако няма конструктор без параметри се появява грешка в манифеста
    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Geocoder geocoder = new Geocoder(this, Locale.US);
        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        String errorMessage = "";
        String cityName = null;
        // Get the location passed to this service through an extra.
        Location location = intent.getParcelableExtra(
                Constants.LOCATION_DATA_EXTRA);

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            // Най-често хвърля Service is not available
            // errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, ioException);
                //ако geocoder.getFromLocation() хвърли Service is not available тогава използвам GoogleMap директно
                cityName = fetchCityNameUsingGoogleMap(location);


        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }
        // Google maps is used!
        if(cityName !=  null  ){
            deliverResultToReceiver(Constants.SUCCESS_RESULT,cityName);
        }else

        {
            // Handle case where no address was found.
            if (addresses == null || addresses.size() == 0) {
                if (errorMessage.isEmpty()) {
                    errorMessage = getString(R.string.no_address_found);
                    Log.e(TAG, errorMessage);
                }
            } else {
                // getFromLocation() е върнал адрес
                Address address = addresses.get(0);
                cityName = address.getSubAdminArea();
//           ArrayList<String> addressFragments = new ArrayList<String>();
//
//             Fetch the address lines using getAddressLine,
//             join them, and send them to the thread.
//            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
//                addressFragments.add(address.getAddressLine(i));
//            }
//
//            deliverResultToReceiver(Constants.SUCCESS_RESULT,
//                    TextUtils.join(System.getProperty("line.separator"),
//                            addressFragments));
                Log.i(TAG, getString(R.string.address_found));
                deliverResultToReceiver(Constants.SUCCESS_RESULT, cityName);
            }
        }
    }

    //---------------------GOOGLE MAP START-----------------------

    private String fetchCityNameUsingGoogleMap(Location location)
    {
        String googleMapUrl = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + location.getLatitude() + ","
                + location.getLongitude() + "&sensor=false&language=fr";

        try
        {
            JSONObject googleMapResponse = new JSONObject(ANDROID_HTTP_CLIENT.execute(new HttpGet(googleMapUrl),
                    new BasicResponseHandler()));

            // many nested loops.. not great -> use expression instead
            // loop among all results
            JSONArray results = (JSONArray) googleMapResponse.get("results");
            for (int i = 0; i < results.length(); i++)
            {
                // loop among all addresses within this result
                JSONObject result = results.getJSONObject(i);
                if (result.has("address_components"))
                {
                    JSONArray addressComponents = result.getJSONArray("address_components");
                    // loop among all address component to find a 'locality' or 'sublocality'
                    for (int j = 0; j < addressComponents.length(); j++)
                    {
                        JSONObject addressComponent = addressComponents.getJSONObject(j);
                        if (result.has("types"))
                        {
                            JSONArray types = addressComponent.getJSONArray("types");

                            // search for locality and sublocality
                            String cityName = null;

                            for (int k = 0; k < types.length(); k++)
                            {
                                if ("locality".equals(types.getString(k)) && cityName == null)
                                {
                                    if (addressComponent.has("long_name"))
                                    {
                                        cityName = addressComponent.getString("long_name");
                                    }
                                    else if (addressComponent.has("short_name"))
                                    {
                                        cityName = addressComponent.getString("short_name");
                                    }
                                }
                                if ("sublocality".equals(types.getString(k)))
                                {
                                    if (addressComponent.has("long_name"))
                                    {
                                        cityName = addressComponent.getString("long_name");
                                    }
                                    else if (addressComponent.has("short_name"))
                                    {
                                        cityName = addressComponent.getString("short_name");
                                    }
                                }
                            }
                            if (cityName != null)
                            {
                                return cityName;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ignored)
        {
            ignored.printStackTrace();
        }
        return null;
    }
    //---------------------GOOGLE MAP END-------------------------

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }
}
