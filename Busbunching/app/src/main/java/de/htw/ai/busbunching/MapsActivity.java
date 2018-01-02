package de.htw.ai.busbunching;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentActivity;
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

import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationHandler.LocationHandlerListener {

    private String host;

    private static final LatLng BERLIN = new LatLng(52.5200066,13.404954);
    private boolean newlyLoaded; // Flag to move camera

    private GoogleMap mMap;
    private GeoJsonLayer layer;
    private LocationHandler locationHandler;

    private static AsyncHttpClient httpClient = new AsyncHttpClient();

    private long routeId;
    private String lineId;

    private boolean destroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        host = getBaseContext().getString(R.string.host);

        routeId = getIntent().getLongExtra("ROUTE", 0);
        lineId = getIntent().getStringExtra("LINE");

        // Obtain the SupportMapFragment
        // and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(1);
        menuItem.setChecked(true);
        navigation.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    Intent intentHome = new Intent(MapsActivity.this, MainActivity.class);
                    startActivity(intentHome);
                    break;
                case R.id.navigation_credits:
                    Intent intentCredits = new Intent(MapsActivity.this, CreditsActivity.class);
                    intentCredits.putExtra("ROUTE", routeId);
                    intentCredits.putExtra("LINE", lineId);
                    startActivity(intentCredits);
                    break;
                default:
                    break;
            }
            return false;
        });

        if (LocationHandler.getInstance() == null) {
            locationHandler = LocationHandler.createInstance(this, 10000);
        }
        locationHandler = LocationHandler.getInstance();
        LocationHandler.getInstance().addListener(this);

        newlyLoaded = true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getRoute(routeId);

        getVehiclesOnRoute();
        startGetVehiclesOnRouteHandler();

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BERLIN, 10));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
        LocationHandler.getInstance().removeListener(this);
    }

    private void getRoute(long id) {
        String url = host + "/api/v1/route/geo/" + id;
        httpClient.get(this, url, new AsyncHttpResponseHandler() {
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

    private void getVehiclesOnRoute() {
        String url = host + "/api/v1/vehicle/" + locationHandler.getDeviceID() + "/list";
        httpClient.get(this, url, new AsyncHttpResponseHandler() {
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

    private void startGetVehiclesOnRouteHandler() {
        Handler handler = new Handler();
        int delay = 10000; //milliseconds

        handler.postDelayed(new Runnable(){
            public void run(){
                getVehiclesOnRoute();
                if (!destroyed) {
                    handler.postDelayed(this, delay);
                }
            }
        }, delay);
    }


    @Override
    public void onLocationUpdate(Location location) {
        // TODO POST mit location MAYBE

//        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//        mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

    }

    @SuppressLint("DefaultLocale")
    private String formatMillisToOutputString (long millis) {
        return String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }


}
