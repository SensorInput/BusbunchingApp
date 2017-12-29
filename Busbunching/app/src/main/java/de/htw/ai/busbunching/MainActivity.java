package de.htw.ai.busbunching;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;


public class MainActivity extends AppCompatActivity implements LocationHandler.LocationHandlerListener {

    private Button start_button;
    private LocationHandler locationHandler;

    private static final String TAG ="MainActivity";
    //Error den wir erhalten wenn der user nicht die korrekte version der Google Play Services besitzt
    private static final int ERROR_DIALOG_REQUEST = 9001;

    /*

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:

                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_map:
                    Intent intentMap = new Intent(MainActivity.this, MapsActivity.class);
                    startActivity(intentMap);
                    mTextMessage.setText(R.string.map);
                    return true;
                case R.id.navigation_credits:
                    Intent intentCredits = new Intent(MainActivity.this, CreditsActivity.class);
                    startActivity(intentCredits);
                    mTextMessage.setText(R.string.credits);
                    return true;
            }
            return false;
        }
    };

    */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //MenuItem an der Stelle 0 und angeben das es ausgewählt ist (setChecked(true)
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(0);
        menuItem.setChecked(true);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.navigation_home:
                        //Hier muss nix hin da wir uns schon in Home befinden
                        break;

                    case R.id.navigation_map:
                        Intent intentMap = new Intent(MainActivity.this, MapsActivity.class);
                        startActivity(intentMap);
                        break;

                    case R.id.navigation_credits:
                        Intent intentCredits = new Intent(MainActivity.this, CreditsActivity.class);
                        startActivity(intentCredits);
                        break;
                }
                return false;
            }
        });

        locationHandler = LocationHandler.createInstance(this, 10000);
        locationHandler.askForPermission(this);
        locationHandler.addListener(this);

        configureButton(this);

       // textView_Coordinates.append("\n "  + locationHandler.getLatitude()+" " + locationHandler.getLongitude());

    }

    public void configureButton (final Context context) {
        start_button = findViewById(R.id.start_button);
        start_button.setOnClickListener(view -> locationHandler.startLocationHandler());

    }

    //Bestätigen das der User die korrekte Version der googlePlayServices besitzt um googlemaps zu nutzen Checking the verison
    public Boolean checkServices() {
        Log.d(TAG,"checkServices: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS){
            //everything is fine alles ok
            Log.d(TAG, "checkServices: Google Play Services is working");
            return true;
        }else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //Error occured but we can resolve it we can fix it
            Log.d(TAG, "checkingServices: an error occured but it can be fixed ");
            //Take the error that was thrown and google will give us a dialog where we can find that solution
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else {
            //We cant solve the Error
            Toast.makeText(this, "You cant make map request", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_SHORT).show();
        //TODO PUT
    }
}
