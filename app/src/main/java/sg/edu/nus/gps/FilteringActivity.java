package sg.edu.nus.gps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.jjoe64.graphview.series.DataPoint;

import java.text.DecimalFormat;
import java.util.Vector;

import static sg.edu.nus.gps.R.id.kalmanNumber;
import static sg.edu.nus.gps.R.id.statusKalman;

public class FilteringActivity extends AppCompatActivity implements SensorEventListener, ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener {

    //----------------
    //VARIABLES
    //----------------
    private static final String TAG = MapsActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;
    private LocationListener locationListener;
    private LocationManager locationManager;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    //for sensors
    private SensorManager sensorManager;
    private SensorEvent event;

    Double gps_var_x, gps_var_y, acc_var_x, acc_var_y;

    //xml variables
    TextView status, number_of_loc;
    Vector<DataPoint> gps_locations, kalman_estimate;

    //other variables
    double degree;
    boolean first = true;
    int locationsNum;
    Intent myIntent;
    boolean while_hold = true;
    double[] new_point;
    TableLayout tableLayout;
    TableRow row, row1;
    TextView rowText, rowText1;
    double xVal, yVal;
    private Sensor accelerometer, magnetic_field;
    Thread sensorReading;
    DataPoint center;
    int kalman_count;

    double[] kalman_array_y, kalman_array_x;

    //KalmanFilterTestObjects
    KalmanFilterTest kalman_x, kalman_y;

    int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtering);
        count = 0;

        status = (TextView) findViewById(statusKalman);
        tableLayout = (TableLayout) findViewById(R.id.tableLayout3);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetic_field = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetic_field, SensorManager.SENSOR_DELAY_NORMAL);

        gps_locations = new Vector<>();
        kalman_estimate = new Vector<>();
        locationsNum = 0;
        center = new DataPoint(1.298732, 103.778344);

        // First we need to check availability of play services
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
        }

        locationSetUp();

        //when taking out outliers
        acc_var_x = 0.47;
        acc_var_y = 0.52;

        gps_var_x = 0.431702;
        gps_var_y = 0.4521355;

        status.setText("Running Kalman Filter");

        //create both KalmanFilterTest Objects
        kalman_x = new KalmanFilterTest(gps_var_x, acc_var_x);
        kalman_y = new KalmanFilterTest(gps_var_y, acc_var_y);

        kalmanStart();

    }

    //--------------------------------------------------------
    //creates the thread to read and calculate every second
    //--------------------------------------------------------
    public void kalmanStart(){
        //creating the thread that controls it for every second
        sensorReading = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    kalman_count = 0;
                    number_of_loc = (TextView) findViewById(kalmanNumber);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            number_of_loc.setText("# of locations: 0");
                            count++;
                        }
                    });
                    while(while_hold){
                        kalman_count++;
                        sensorReading();
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //does the actual calculations
            public void sensorReading() {
                if (event != null) {
                    yVal = Math.cos(degree_to_rad(degree)) * Math.sqrt((event.values[0]*event.values[0]) + (event.values[1]*event.values[1]));
                    xVal = Math.sin(degree_to_rad(degree)) * Math.sqrt((event.values[0]*event.values[0]) + (event.values[1]*event.values[1]));

                    if (mLastLocation != null) {
                        //convert LatLng to x&y
                        double latitude = mLastLocation.getLatitude();
                        double longitude = mLastLocation.getLongitude();

                        new_point = new double[2];
                        new_point = ll_to_xy(center.getX(),center.getY(),latitude,longitude);
                        double xLong = new_point[0];
                        double yLat = new_point[1];

                        //add x & y respectively to the vectors
                        if (first == true) {
                            first = false;
                            gps_locations.add(new DataPoint(xLong, yLat));
                            if(kalman_count >= 8){
                                kalman_estimate.add(new DataPoint(xLong, yLat));
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    number_of_loc.setText("# of locations: " + locationsNum);
                                }
                            });
                        }
                        else {
                            //conduct kalman estimate
                            kalman_array_x = (kalman_x.getEstimate(xVal, xLong, xLong - gps_locations.get(gps_locations.size()-1).getX()));
                            kalman_array_y = kalman_y.getEstimate(yVal, yLat, yLat - gps_locations.get(gps_locations.size()-1).getY());
                            gps_locations.add(new DataPoint(xLong, yLat));
                            if(kalman_count >= 8){
                                //change the number of locations to +1
                                locationsNum++;
                                kalman_estimate.add(new DataPoint(kalman_array_x[0], kalman_array_y[0]));
                                tableFill();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        number_of_loc.setText("# of locations: " + locationsNum);
                                        rowText.setText("Expected Y: " +
                                                Double.parseDouble(new DecimalFormat("##.####").format(kalman_array_y[0]))
                                                + "m" + ", Expected X: "
                                                + Double.parseDouble(new DecimalFormat("##.####").format(kalman_array_x[0])) + "m");
                                        rowText1.setText("Orientation: " + Double.parseDouble(new DecimalFormat("##").format(degree))
                                                + "\u00b0");
                                        row.addView(rowText);
                                        row1.addView(rowText1);
                                        tableLayout.addView(row);
                                        tableLayout.addView(row1);
                                    }
                                });
                            }
                        }
                    } else{
                        //status.setText("location is null");
                    }
                } else{
                    //status.setText("event is null");
                }
            }
        });
        sensorReading.start();
    }


    //---------------------------------
    // Helper Functions
    //---------------------------------

    public void tableFill(){
        row = new TableRow(this);
        row1 = new TableRow(this);
        rowText = new TextView(this);
        rowText1 = new TextView(this);
    }


    //----------------------------------------------
    //LatLng to XY Converter Helper Functions
    //----------------------------------------------
    public double[] ll_to_xy(double orig_long,double orig_lat,double new_long,double new_lat){
        double rotation_angle = 0;
        double xx,yy,r,ct,st,angle;

        angle = degree_to_rad(rotation_angle);

        xx = (new_long-orig_long)*meters_deg_long(orig_lat);
        yy = (new_lat-orig_lat)*meters_deg_lat(orig_lat);

        r = Math.sqrt(xx*xx + yy*yy);

        if(r != 0){
            ct = xx/r;
            st = yy/r;
            xx = r * (ct*Math.cos(angle) + st*Math.cos(angle));
            yy = r * (st*Math.cos(angle) - ct*Math.cos(angle));
        }

        double[] vals = new double[2];
        vals[0] = xx;
        vals[1] = yy;
        return vals;
    }

    double degree_to_rad(double deg1){
        return deg1/57.2957795;
    }

    double meters_deg_long(double d){
        double d2r = degree_to_rad(d);
        return ((111415.13 * Math.cos(d2r)) - (94.55 * Math.cos(3.0*d2r)) + (0.12 * Math.cos(5.0*d2r)));

    }

    double meters_deg_lat(double d){
        double d2r = degree_to_rad(d);
        return (111132.09 - (566.05 * Math.cos(2.0*d2r)) + (1.20 * Math.cos(4.0*d2r)) - (0.002 * Math.cos(6.0 * d2r)));
    }

     //--------------------
    //Sensor functions
    //---------------------

    @Override
    public void onSensorChanged(SensorEvent eventRead) {
        if(eventRead.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            //number_of_loc.setText("in acc read");
            event = eventRead;
        }
        else if(eventRead.sensor.getType() == Sensor.TYPE_ORIENTATION){
            //number_of_loc.setText("in orientaion read");
            degree = eventRead.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //--------------------
    //Location functions
    //---------------------
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public void locationSetUp(){
        //setting up location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 1, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 200, 1, this);
    }

    //------------------------
    //OnClick Functions
    //------------------------

    public void kalmanStop(View view){
        sensorManager.unregisterListener(this);
        if(sensorReading.isAlive()){
            while_hold=false;
            try {
                sensorReading.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        myIntent = new Intent(this, FilterGraphActivity.class);
        //add the 2 extras and start
        myIntent.putExtra("gps", gps_locations);
        myIntent.putExtra("fixed", kalman_estimate);
        startActivity(myIntent);
    }

    public void goToMainActivity(View view) {
        if(sensorReading != null) {
           if (sensorReading.isAlive()) {
               while_hold = false;
               try {
                   sensorReading.join();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }

           sensorManager.unregisterListener(this);
        }
        finish();
    }

    //-----------------------------------------------
    //FOR THE DIFFERENT STAGES IN ACTIVITY LIFECYCLE
    //-----------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();


        checkPlayServices();
    }

    //-----------------------------------
    //REPEAT GOOGLE PLAY
    //-----------------------------------
    //creating google api client
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    // Method to verify google play services on the device
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }


}
