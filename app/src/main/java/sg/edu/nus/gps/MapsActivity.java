package sg.edu.nus.gps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

import static sg.edu.nus.gps.R.id.coordinates;
import static sg.edu.nus.gps.R.id.readingNumber1;

public class MapsActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener {

    //----------------
    //VARIABLES
    //----------------

    // LogCat tag
    private static final String TAG = MapsActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    //my variables
    private Vector<DataPoint> locations;
    private TextView lat_lng;
    private TextView number_of_loc;
    private Button show_location;
    private Intent myIntent;
    private TableLayout tableLayout;
    private DataPoint center;
    double[] new_point;
    private LocationListener locationListener;
    private LocationManager locationManager;

    //---------------------
    //ONCREATE
    //instantiates objects and checks permissions
    //---------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //my variables
        lat_lng = (TextView) findViewById(coordinates);
        number_of_loc = (TextView) findViewById(readingNumber1);
        tableLayout = (TableLayout) findViewById(R.id.tableLayout1);
        locations = new Vector<>();
        show_location = (Button) findViewById(R.id.button);
        number_of_loc.setText("0");
        center = new DataPoint(1.298732, 103.778344);

        // First we need to check availability of play services
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
        }

        // Show location button click listener
        show_location.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                displayLocation();
            }
        });

        //setting up location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
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

    //------------------------------------
    //set up location listener & helpers
    //------------------------------------

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("onLocationChanged!!!");
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

    //---------------------------------------
    //OnClick Functions
    //---------------------------------------

    //display location on the UI once the button is clicked
    private void displayLocation() {
        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            new_point = new double[2];
            new_point = ll_to_xy(center.getX(),center.getY(),latitude,longitude);

            lat_lng.setText("X: " + Double.parseDouble(new DecimalFormat("##.###").format(new_point[0]))
                    + "m, Y: " + Double.parseDouble(new DecimalFormat("##.###").format(new_point[1])) + "m");

            TableRow row = new TableRow(this);
            TextView rowText = new TextView(this);
            rowText.setText("X: " + Double.parseDouble(new DecimalFormat("##.###").format(new_point[0]))
                    + "m, Y: " + Double.parseDouble(new DecimalFormat("##.###").format(new_point[1])) + "m");
            row.addView(rowText);
            tableLayout.addView(row);

            //add to the location vector
            locations.add(new DataPoint(new_point[0],new_point[1]));

            //change the number of locations to +1
            String number_loc_string = number_of_loc.getText().toString();
            int number_loc_int = Integer.parseInt(number_loc_string);
            number_loc_int++;
            number_loc_string = Integer.toString(number_loc_int);
            number_of_loc.setText(number_loc_string);

        } else {

            lat_lng.setText("Error. Please Reset.");
        }
    }

    public void onClick_get_mean(View view){
        //new activity to display
        myIntent = new Intent(this, GPSMapActivity.class);
        myIntent.putExtra("vector", locations);
        startActivity(myIntent);
    }

    public void onClick_clear_history(View view){
        //delete the data within the vector and table
        locations.clear();
        tableLayout.removeAllViews();
        number_of_loc.setText("0");
        lat_lng.setText("");
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
        return ((111415.13 * Math.cos(d2r)) - (94.55 * Math.cos(3.0*d2r))
                + (0.12 * Math.cos(5.0*d2r)));
    }
    double meters_deg_lat(double d){
        double d2r = degree_to_rad(d);
        return (111132.09 - (566.05 * Math.cos(2.0*d2r)) + (1.20 * Math.cos(4.0*d2r))
                - (0.002 * Math.cos(6.0 * d2r)));
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

    public void goToMainActivity(View view) {

        // please fill in your code here.  Remember to stop drawing before you return to the MainActivity.
        finish();

    }

    //--------------------------------
    //CHECKING GOOGLE PLAY SERVICES
    //--------------------------------

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
        // Once connected with google api, get the location
        //displayLocation();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

}
