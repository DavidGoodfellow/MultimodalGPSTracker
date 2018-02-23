package sg.edu.nus.gps;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    Intent myIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //create shared preferences and leave them all blank
        SharedPreferences sharedPreferences = getSharedPreferences("MyData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //for gps
        editor.putString("gps_x_mean1","");
        editor.putString("gps_y_mean1","");
        editor.putString("gps_x_variance1","");
        editor.putString("gps_y_variance1","");
        editor.putString("gps_x_mean2","");
        editor.putString("gps_y_mean2","");
        editor.putString("gps_x_variance2","");
        editor.putString("gps_y_variance2","");
        editor.putString("gps_x_mean3","");
        editor.putString("gps_y_mean3","");
        editor.putString("gps_x_variance3","");
        editor.putString("gps_y_variance3","");
        editor.putString("gps_x_mean4","");
        editor.putString("gps_y_mean4","");
        editor.putString("gps_x_variance4","");
        editor.putString("gps_y_variance4","");

        //for accelerometer
        editor.putString("acc_x_mean1","");
        editor.putString("acc_y_mean1","");
        editor.putString("acc_x_variance1","");
        editor.putString("acc_y_variance1","");

        editor.commit();
    }

    public void activity1(View view){
        //new activity to display this!!

        myIntent = new Intent(this, MapsActivity.class);
        startActivity(myIntent);
    }

    public void activity2(View view){
        //new activity to display this!!

        myIntent = new Intent(this, AccelerometerActivity.class);
        startActivity(myIntent);
    }

    public void activity3(View view){
        //new activity to display this!!

        myIntent = new Intent(this, FilteringActivity.class);
        startActivity(myIntent);
    }
}
