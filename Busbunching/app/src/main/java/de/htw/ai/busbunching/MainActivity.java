package de.htw.ai.busbunching;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity implements LocationHandler.LocationHandlerListener {

    private static final String CONFIG = "config.txt";
    private String host;

    private boolean destroyed;

    private Button start_button;
    private EditText busline_text;
    private TextView frontVehicle;
    private TextView vehicleBehind;
    private ImageView vehicleFrontImage;
    private ImageView vehicleBackImage;

    private LocationHandler locationHandler;

    private Route currentRoute;
    private String currentLineId;
    private long currentRouteId;
    private String busLineId;
    private String deviceId = "";

    static boolean isStarted;

    private static AsyncHttpClient httpClient = new AsyncHttpClient();

    private static final String TAG = "MainActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        host = getBaseContext().getString(R.string.host);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frontVehicle = findViewById(R.id.textViewVehicleBehind);
        vehicleBehind = findViewById(R.id.textViewFrontVehicle);

        vehicleFrontImage = findViewById(R.id.imageViewFrontVehicle);
        vehicleBackImage = findViewById(R.id.imageViewVehicleBehind);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        //MenuItem an der Stelle 0 und angeben das es ausgewählt ist (setChecked(true)
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(0);
        menuItem.setChecked(true);
        navigation.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_map:
                    Intent intentMap = new Intent(MainActivity.this, MapsActivity.class);
                    intentMap.putExtra("ROUTE", currentRouteId);
                    intentMap.putExtra("LINE", currentLineId);
                    intentMap.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intentMap);
                    break;
                case R.id.navigation_credits:
                    Intent intentCredits = new Intent(MainActivity.this, CreditsActivity.class);
                    intentCredits.putExtra("ROUTE", currentRouteId);
                    intentCredits.putExtra("LINE", currentLineId);
                    startActivity(intentCredits);
                    break;
                default:
                    break;
            }
            return false;
        });

        locationHandler = LocationHandler.createInstance(this, 10000);
        locationHandler.askForPermission(this);
        locationHandler.addListener(this);
        deviceId = getDeviceId();
        locationHandler.setDeviceID(deviceId);

        configureButton(this);
    }

    private void configureButton(final Context context) {
        start_button = findViewById(R.id.start_button);
        busline_text = findViewById(R.id.busline);

        start_button.setOnClickListener(view -> {
            if (isStarted) {
                start_button.setText("START");
                currentRouteId = -1;
                try {
                    putRouteDetail();
                } catch (JSONException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                locationHandler.stopLocationHandler();
            } else {
                start_button.setText("STOP");
                busLineId = busline_text.getText().toString();
                busline_text.setText("");

                getRouteDetail(busLineId);
                locationHandler.startLocationHandler();
            }
            isStarted = !isStarted;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocationHandler.getInstance().removeListener(this);
    }

    //Bestätigen das der User die korrekte Version der googlePlayServices besitzt um googlemaps zu nutzen Checking the verison
    public Boolean checkServices() {
        Log.d(TAG, "checkServices: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine alles ok
            Log.d(TAG, "checkServices: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //Error occured but we can resolve it we can fix it
            Log.d(TAG, "checkingServices: an error occured but it can be fixed ");
            //Take the error that was thrown and google will give us a dialog where we can find that solution
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            //We cant solve the Error
            Toast.makeText(this, "checkServices: You cant make map request", Toast.LENGTH_SHORT).show();
            return false;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 101: {
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED)) {
                    if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
                        Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    public void onLocationUpdate(Location location) {
        Toast.makeText(MainActivity.this, String.format("Get Location:  %f %f", location.getLongitude(), location.getLatitude()), Toast.LENGTH_LONG).show();
        try {
            updateCurrentPosition(location);
            getVehiclesOnRoute();
        } catch (JSONException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationStart() {
        destroyed = false;
        startGetVehiclesOnRouteHandler();
    }

    @Override
    public void onLocationStop() {
        destroyed = true;
    }

    private void startGetVehiclesOnRouteHandler() {
        Handler handler = new Handler();
        int delay = 10000; //milliseconds

        handler.postDelayed(new Runnable() {
            public void run() {
                getVehiclesOnRoute();
                if (!destroyed) {
                    handler.postDelayed(this, delay);
                }
            }
        }, delay);
    }

    private void getRouteDetail(String ref) {
        String url = host + "/api/v1/route/" + ref;
        httpClient.get(this, url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<Route>>() {
                }.getType();

                List<Route> routes = gson.fromJson(new String(responseBody), type);
                showDialog(routes);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                System.out.println("Failed to load routes detail: " + statusCode);
            }
        });
    }

    private void getVehiclesOnRoute() {
        String url = host + "/api/v1/vehicle/" + deviceId + "/list";
        System.out.println(url);
        httpClient.get(this, url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONArray allVehicleOnRoute = new JSONArray(new String(responseBody));
                    for (int i = 0; i < allVehicleOnRoute.length(); i++) {
                        JSONObject jsonVehicle = allVehicleOnRoute.getJSONObject(i);
                        if (jsonVehicle.getString("ref").equals(locationHandler.getDeviceID())) {
                            /*
                            if(i<=0) {
                                //Set textview vor mir auf 0; bzw bus verschwinden lassen
                            }
                            if(i+1 > allVehicleOnRoute.length()) {
                                //set textview hinter mir auf 0; bzw bus verschwinden lassen
                            }
                            */
                            if (i - 1 >= 0) {
                                JSONObject jsonVehicleFront = allVehicleOnRoute.getJSONObject(i - 1);

                                int relTimeDistFront = jsonVehicleFront.getInt("relativeTimeDistance");
                                String relTimeDistFrontString = formatMillisToOutputString(relTimeDistFront);
                                MainActivity.this.frontVehicle.setText(relTimeDistFrontString);
                            } else {
                                MainActivity.this.frontVehicle.setText("No Vehicle");
                            }
                            if (i + 1 < allVehicleOnRoute.length()) {
                                JSONObject jsonVehicleBehind = allVehicleOnRoute.getJSONObject(i + 1);
                                int relTimeDistBehind = jsonVehicleBehind.getInt("relativeTimeDistance");
                                String relTimeDistBehindString = formatMillisToOutputString(relTimeDistBehind);
                                MainActivity.this.vehicleBehind.setText(relTimeDistBehindString);
                            } else {
                                MainActivity.this.vehicleBehind.setText("No Vehicle");
                            }

                            /* Beispiel
                            if(relTimeDistFront < 7) {
                                vehicleFrontImage.setBackgroundColor(200);
                            }
                            */
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                System.out.println("GET Failed " + statusCode);
            }
        });
    }


    private void putRouteDetail() throws JSONException, UnsupportedEncodingException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("ref", deviceId);
        jsonParam.put("routeId", currentRouteId);
        jsonParam.put("time", System.currentTimeMillis());

        String url = host + "/api/v1/vehicle/" + deviceId;
        httpClient.put(this, url, new StringEntity(jsonParam.toString()), "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                System.out.println("PUT Route detail success");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                System.out.println("PUT Route detail Failed " + statusCode);
            }
        });
    }


    private void updateCurrentPosition(final Location location) throws JSONException, UnsupportedEncodingException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("ref", currentLineId);
        jsonParam.put("routeId", currentRouteId);
        jsonParam.put("time", System.currentTimeMillis());
        JSONObject positionParam = new JSONObject();
        positionParam.put("lng", location.getLongitude());
        positionParam.put("lat", location.getLatitude());
        jsonParam.put("position", positionParam);

        String url = host + "/api/v1/vehicle/" + deviceId;
        httpClient.put(this, url, new StringEntity(jsonParam.toString()), "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                System.out.println("PUT UPDATE success");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                System.out.println("PUT UPDATE Failed url: " + url + ", Status: " + statusCode);
            }
        });
    }

    private void postDeviceId(String id) throws JSONException, UnsupportedEncodingException {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("ref", id);

        String url = host + "/api/v1/vehicle";
        httpClient.post(this, url, new StringEntity(jsonParam.toString()), "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                System.out.println("POST device success");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                System.out.println("POST device Failed " + statusCode);
            }
        });
    }

    private void showDialog(List<Route> routes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View view = getLayoutInflater().inflate(R.layout.dialog_route, null);

        CharSequence[] routeNames = new CharSequence[routes.size()];
        for (int i = 0; i < routes.size(); i++) {
            routeNames[i] = routes.get(i).getName();
        }

        builder.setItems(routeNames, (dialog, i) -> {
            currentRoute = routes.get(i);
            currentRouteId = currentRoute.getId();
            currentLineId = currentRoute.getRef();
            try {
                putRouteDetail();
            } catch (JSONException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            routes.clear();
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(dialog1 -> routes.clear());
        dialog.show();
    }

    /*
    Settings
     */

    private String getDeviceId() {
        String deviceId = readDeviceIdFromFile(this);
        if (deviceId.isEmpty()) {
            @SuppressLint("HardwareIds") String id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            writeDeviceIdToFile(id, this);
            try {
                postDeviceId(id);
            } catch (JSONException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return id;
        }
        return deviceId;
    }

    private void writeDeviceIdToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(CONFIG, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String readDeviceIdFromFile(Context context) {
        String ret = "";
        try {
            InputStream inputStream = context.openFileInput(CONFIG);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }
        return ret;
    }

    @SuppressLint("DefaultLocale")
    private String formatMillisToOutputString(long millis) {
        return String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

}
