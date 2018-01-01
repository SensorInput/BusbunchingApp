package de.htw.ai.busbunching;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.LOCATION_SERVICE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;


public class LocationHandler implements LocationListener {

    //Location service
    private LocationManager locationManager;
    //Listen for location changes
    private String URL_ADDRESS = "http://h2650399.stratoserver.net:4545/position";
    private String deviceID;
    private Context context;
    private double latitude;
    private double longitude;
    private int interval;
    private static final int REQUEST_CODE = 101;

    // Singleton
    private static LocationHandler INSTANCE;

    public static LocationHandler createInstance(Context context, int interval) {
        if (INSTANCE == null) {
            INSTANCE = new LocationHandler(context, interval);
        }
        return INSTANCE;
    }

    public static LocationHandler getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("Instance is null");
        }
        return INSTANCE;
    }

    // Listener
    private List<LocationHandlerListener> listeners = new ArrayList<>();

    public void addListener(LocationHandlerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LocationHandlerListener listener) {
        listeners.remove(listener);
    }

    public interface LocationHandlerListener {
        void onLocationUpdate(Location location);
    }

    private LocationHandler(Context context, int interval) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        this.interval = interval;
        this.deviceID = Secure.getString(context.getContentResolver(),
                Secure.ANDROID_ID);
    }


    public void startLocationHandler() {

        if (ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //String array mit Permission die wir in die AndroidManifest datei geschrieben haben, request code kann eine randomnummer sein, wichtig aber fuer "onRequestPermissionResult
                requestOnPermissions(new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        , 101);
            }
            return;
        }
        locationManager.requestLocationUpdates("gps", interval, 0, this);
    }

    private void requestOnPermissions(String[] strings, int i) {
        switch (i) {
            case 101:
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(intent);
                break;
            default:
                break;
        }
    }


    @Override
    public void onLocationChanged(Location location) {

        System.out.println("LOCATION: " + location.getLatitude() + " " + location.getLongitude());

        for (LocationHandlerListener listener : listeners) {
            listener.onLocationUpdate(location);
        }

        /*
        try {
            sendPost(location);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        */

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);

    }

    private static AsyncHttpClient httpClient = new AsyncHttpClient();

    private void sendPost(final Location location) throws JSONException, UnsupportedEncodingException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("lat", location.getLatitude());
        jsonParam.put("lng", location.getLongitude());
        jsonParam.put("deviceID", deviceID);
        jsonParam.put("time", System.currentTimeMillis());

        httpClient.post(context, URL_ADDRESS, new StringEntity(jsonParam.toString()), "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                System.out.println("Post success");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                System.out.println("Failed success " + statusCode);
            }
        });
    }



    public String getDeviceID() {
        return deviceID;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void askForPermission(Activity activity) {
        // Ask for permission
        if (ContextCompat.checkSelfPermission(activity, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(activity, new String[]{ACCESS_FINE_LOCATION}, REQUEST_CODE);
            }
        }
    }

}
