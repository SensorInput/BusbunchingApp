package de.htw.ai.busbunching;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

public class CreditsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);

        long journeyID = getIntent().getLongExtra("ROUTE", 0);
        String routeID = getIntent().getStringExtra("LINE");

        BottomNavigationView navigation = findViewById(R.id.navigation);
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(2);
        menuItem.setChecked(true);
        navigation.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    Intent intentHome = new Intent(CreditsActivity.this, MainActivity.class);
                    startActivity(intentHome);
                    break;

                case R.id.navigation_map:
                    Intent intentMap = new Intent(CreditsActivity.this, MapsActivity.class);
                    intentMap.putExtra("ROUTE", journeyID);
                    intentMap.putExtra("LINE", routeID);
                    startActivity(intentMap);
                    break;

                default:
                    break;
            }
            return false;
        });
    }
}
