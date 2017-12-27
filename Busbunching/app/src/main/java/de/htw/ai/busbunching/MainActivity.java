package de.htw.ai.busbunching;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.time.Clock;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private Button start_button;
    private TextView textView_Coordinates;
    private LocationHandler locationHandler;

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

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        //MenuItem an der Stelle 0 und angeben das es ausgewählt ist (setChecked(true)
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(0);
        menuItem.setChecked(true);

        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.navigation_home:
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

        locationHandler = new LocationHandler(this, 10000);


        //textView_Coordinates = (TextView) findViewById(R.id.textView_Coordinates);

        //configureButton(this);

       // textView_Coordinates.append("\n "  + locationHandler.getLatitude()+" " + locationHandler.getLongitude());

    }

    public void configureButton (final Context context) {

        start_button = (Button) findViewById(R.id.start_button);


        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //String array mit Permission die wir in die AndroidManifest datei geschrieben haben, request code kann eine randomnummer sein, wichtig aber fuer "onRequestPermissionResult
                requestOnPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                        ,101);
            }
            return;
        }
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationHandler.startLocationHandler();
            }
        });

    }

    public void requestOnPermissions(String[] strings, int requestCode) {
        switch (requestCode){
            case 101:
                configureButton(this);
                break;
            default:
                break;
        }
    }

}
