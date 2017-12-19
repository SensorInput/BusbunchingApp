package de.htw.ai.busbunching;

import android.Manifest;
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
    private int interval;

    public LocationHandler(Context context, int interval) {
        this.locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        this.interval = interval;
        this.deviceID = Secure.getString(context.getContentResolver(),
                Secure.ANDROID_ID);
    }


    public void startLocationHandler() {

        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //String array mit Permission die wir in die AndroidManifest datei geschrieben haben, request code kann eine randomnummer sein, wichtig aber fuer "onRequestPermissionResult
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                        ,101);
            }
            return;
        }
        locationManager.requestLocationUpdates("gps", interval, 0, this);
    }

    private void requestPermissions(String[] strings, int requestCode) {
        switch (requestCode){
            case 101:
                startLocationHandler();
                break;
            default:
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            sendPost(location);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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

    public void setLatitude(Location latitude) {
        this.latitude = latitude.getLatitude();
    }

    private static AsyncHttpClient httpClient = new AsyncHttpClient();

    public void sendPost(final Location location) throws JSONException, UnsupportedEncodingException {
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
}
