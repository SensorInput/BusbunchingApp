package de.htw.ai.busbunching;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationHandler.LocationHandlerListener {

    private GoogleMap mMap;
    private GeoJsonLayer layer;
    private static AsyncHttpClient httpClient = new AsyncHttpClient();
    private ArrayList<String> arrayList = new ArrayList<>();
//    private String URL_ADDRESS = "http://h2650399.stratoserver.net:4545/position";
//    private String deviceID;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(1);
        menuItem.setChecked(true);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        Intent intentHome = new Intent(MapsActivity.this, MainActivity.class);
                        startActivity(intentHome);
                        break;
                    case R.id.navigation_map:
                        break;
                    case R.id.navigation_credits:
                        Intent intentCredits = new Intent(MapsActivity.this, CreditsActivity.class);
                        startActivity(intentCredits);
                        break;
                }
                return false;
            }
        });

        if (LocationHandler.getInstance() == null) {
            LocationHandler.createInstance(this, 10000);
        }
        LocationHandler.getInstance().addListener(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getRoute(68);
        startGetVehiclesOnRouteHandler(68);

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        LatLng berlin = new LatLng(52.4, 13.5);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(berlin));
    }

    private void getLocationPermission() {
        //String[] persmission = {Mainfe}
    }

    private void getRoute(int id) {
        httpClient.get(this, "http://h2650399.stratoserver.net:4545/api/v1/route/geo/" + id, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONObject jsonParam = new JSONObject(new String(responseBody));

                    Handler mainHandler = new Handler(MapsActivity.this.getMainLooper()) ;
                    Runnable runnable  = ()-> {
                        layer = new GeoJsonLayer(mMap, jsonParam);
                        layer.addLayerToMap();
                    };
                    mainHandler.post(runnable);
                    System.out.println(jsonParam);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                System.out.println("Failed success " + statusCode);
            }
        });
    }

    private void getVehiclesOnRoute(int id) {
        httpClient.get(this, "http://h2650399.stratoserver.net:4545/api/v1/vehicle/636c81cc2361acd7/list", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONArray jsonArray = new JSONArray(new String(responseBody));
                    //JSONObject jsonParam = new JSONObject(new String(responseBody));

                    Handler mainHandler = new Handler(MapsActivity.this.getMainLooper()) ;
                    Runnable runnable  = ()-> {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                JSONObject jsonObj = jsonArray.getJSONObject(i);
                                System.out.println(jsonObj);
                                JSONObject jsonObject = jsonArray.getJSONObject(i).getJSONObject("geoLngLat");

                                System.out.println("lat: " + jsonObject.getDouble("lat"));
                                LatLng latLng = new LatLng(jsonObject.getDouble("lat"), jsonObject.getDouble("lng"));
                                if (jsonObj.getDouble("relativeDistance") == 0) {
                                    mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)) );
                                } else {
                                    mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) );
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    mainHandler.post(runnable);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                System.out.println("Failed success " + statusCode);
            }
        });
    }

    private void startGetVehiclesOnRouteHandler(int routeId) {
        Handler handler = new Handler();
        int delay = 10000; //milliseconds

        handler.postDelayed(new Runnable(){
            public void run(){
                System.out.println("TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT");
                getVehiclesOnRoute(routeId);
                handler.postDelayed(this, delay);
            }
        }, delay);
    }


    @Override
    public void onLocationUpdate(Location location) {
        // TODO POST mit location MAYBE

//        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//        mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) );

    }
}
