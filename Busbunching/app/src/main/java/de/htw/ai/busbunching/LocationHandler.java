package de.htw.ai.busbunching;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import static android.content.Context.LOCATION_SERVICE;

import android.provider.Settings.Secure;

/**
 *
 *
 *
 */

public class LocationHandler implements LocationListener {

    //Location service
    private LocationManager locationManager;
    //Listen for location changes
    private String URL_ADDRESS = "http://h2650399.stratoserver.net:4545/position";
    private int ID = 1;
    private Date time = Calendar.getInstance().getTime();
    private String deviceID;
    private Context context;
    private double latitude;
    private double longitude;
    private int interval;

    public LocationHandler(Context context, int interval) {
        this.locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        this.interval = interval;
        this.deviceID = Secure.getString(context.getContentResolver(),
                Secure.ANDROID_ID);
    }


    @SuppressLint("MissingPermission")
    public void startLocationHandler() {

        locationManager.requestLocationUpdates("gps", interval, 0, this);
    }


    @Override
    public void onLocationChanged(Location location) {
        //setLatitude(location.getLatitude());
        //setLongitude(location.getLongitude());
        System.out.println("LOCATION: "+location.getLatitude()+" "+location.getLongitude());


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
}
