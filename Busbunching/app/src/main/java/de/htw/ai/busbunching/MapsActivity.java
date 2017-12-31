package de.htw.ai.busbunching;

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


import com.google.maps.android.geojson.GeoJsonLayer;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationHandler.LocationHandlerListener {

    private static final LatLng BERLIN = new LatLng(52.5200066,13.404954);
    private boolean newlyLoaded;
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

        newlyLoaded = true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getRoute(68);
        startGetVehiclesOnRouteHandler(68);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BERLIN, 10));
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
                                    if (newlyLoaded) {
                                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                        mMap.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);
                                        newlyLoaded = false;
                                    }
                                    mMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                                } else {
                                    double relDist = jsonObj.getDouble("relativeDistance");
                                    relDist = ((double)((int)(relDist * 100))) / 100;
                                    int relTimeDist = jsonObj.getInt("relativeTimeDistance");
                                    String relTimeDistString = formatMillisToOutputString(relTimeDist);
                                    mMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                            .title(relTimeDistString)
                                            .snippet(String.valueOf(relDist) + " Meter"));
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

    private String formatMillisToOutputString (long millis) {
        return String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }
}
