package com.example.weathernotifier;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements LocationListener {

    Float threshold;

    EditText threshInput;
    EditText latInput;
    EditText lonInput;

    private static final int REQUEST_LOCATION = 1;
    public String latitude;
    public String longitude;

    Intent intentThatCalled;
    public LocationManager locationManager;
    public Criteria criteria;
    public String bestProvider;

    String voice2text;

    private Handler mTimerHandler = new Handler();

    RadioGroup rgDegree;
    RadioGroup threshOption;
    RadioButton radioButton;
    CheckBox checkbox;

    private final int REQUEST_LOCATION_PERMISSION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


//      This is For Push notifications
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestLocationPermission();
        createNotificationChannel();
        intentThatCalled = getIntent();
        voice2text = intentThatCalled.getStringExtra("v2txt");

        threshInput = findViewById(R.id.tempThreshold);
        rgDegree = findViewById(R.id.tempMeasurement);
        threshOption = findViewById(R.id.threshOption);
        checkbox = findViewById(R.id.gpsCheck);
        latInput = findViewById(R.id.latInput);
        lonInput = findViewById(R.id.lonInput);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if(EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
        else {
            EasyPermissions.requestPermissions(this, "Please grant the location permission", REQUEST_LOCATION_PERMISSION, perms);
        }
    }


    /** Called when the user touches the button */
    public void sendMessage(View view) {

        // We need this to be on a thread lest the app freak out and think that it is frozen
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                checkTempsThread(view);
            }
        });
        t.start();
    }


    public void checkTempsThread(View view) {


        // Get the stuff inside the above/below radio buttons
        int radioId;
        radioId = threshOption.getCheckedRadioButtonId();
        radioButton = findViewById(radioId);

        System.out.println("Checking for temps " + radioButton.getText());

        int threshNum = 1;

        if (radioButton.getText().charAt(0) == 'A')
            threshNum = 1;
        if (radioButton.getText().charAt(0) == 'B')
            threshNum = 2;


        // This is to get the stuff inside the celsius/fahrenheit radio buttons
        radioId = rgDegree.getCheckedRadioButtonId();
        radioButton = findViewById(radioId);

        System.out.println("Temperature will be measured in " + radioButton.getText());

        String tempUnits = "standard";

        if (radioButton.getText().charAt(1) == 'F')
            tempUnits = "imperial";
        if (radioButton.getText().charAt(1) == 'C')
            tempUnits = "metric";


        // Get the threshold out of the editable text box
        String cheese = threshInput.getText().toString();

//        System.out.println("Cheese contains '" + cheese + "'");

        // Make sure there's actually something in there
        if (cheese.isEmpty()) {
//            System.out.println("The String is empty");

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Please Enter a Threshold",
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            });

            return;
        }

        // now take the string and put it in the threshold variable as a float
        threshold = Float.parseFloat(cheese);

        // Get the GPS location from the user
        // if the user didn't provide anything, return
        if (!getLocation()) {
            return;
        }


        // This is for the Foreground Service
        Intent serviceIntent = new Intent(this, TempCheck.class);
        serviceIntent.putExtra("threshold", threshold);
        serviceIntent.putExtra("threshOption", threshNum);
        serviceIntent.putExtra("lat", latitude);
        serviceIntent.putExtra("lon", longitude);
        serviceIntent.putExtra("tempUnits", tempUnits);

        // This is where the fun begins
        startForegroundService(serviceIntent);
    }

    // This was some code I found online for displyaing a unix date time in a more readable format
    public void displayDateTime(int unix_seconds) {
        //Unix seconds
        //convert seconds to milliseconds
        Date date = new Date(unix_seconds * 1000L);
        // format of the date
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        jdf.setTimeZone(TimeZone.getTimeZone("GMT-6"));
        String java_date = jdf.format(date);
        System.out.println("\n" + java_date + "\n");
    }


    // A hard coded (for now at least) notification channel through which the app can send the
    // notification
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "tempChannel";
            String description = "Channel for letting the user know threshold has been reached";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("temp", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    public static boolean isLocationEnabled(Context context) {
        //...............
        return true;
    }

    protected boolean getLocation() {

        if (checkbox.isChecked()) {
            if (isLocationEnabled(MainActivity.this)) {
                locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                criteria = new Criteria();
                bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();

                //You can still do this if you like, you might get lucky:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //  TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
//                    return false;

                }
                Location location = locationManager.getLastKnownLocation(bestProvider);
                if (location != null) {
                    Log.e("TAG", "GPS is on");
                    double lon = location.getLongitude();
                    double lat = location.getLatitude();
                    latitude = String.valueOf(lat);
                    longitude = String.valueOf(lon);
                    System.out.println("Your Location: " + "\n" + "Latitude: " + latitude + "\n" + "Longitude: " + longitude);
                    searchNearestPlace(voice2text);
                } else {
                    //This is what you need:
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return false;
                    }
                    new Handler(Looper.getMainLooper()).post(() -> {
                        locationManager.requestLocationUpdates(bestProvider, 1000, 0, this);
                    });
                }
            }
            else
            {
                //prompt user to enable location....
                //.................
            }
        }

        else {

            latitude = latInput.getText().toString();
            longitude = lonInput.getText().toString();

            if (latitude.isEmpty() || longitude.isEmpty()) {
//                System.out.println("The String is empty");

                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Please Enter Latitude and Longitude!",
                            Toast.LENGTH_LONG);
                    toast.show();
                });

                return false;
            }

        }


        System.out.println("Your Location: " + "\n" + "Latitude: " + latitude + "\n" + "Longitude: " + longitude);
//
        return true;

    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);

    }

    @Override
    public void onLocationChanged(Location location) {
        //Hey, a non null location! Sweet!

        //remove location callback:
        locationManager.removeUpdates(this);

        //open the map:
        double lon = location.getLongitude();
        double lat = location.getLatitude();
        latitude = String.valueOf(lat);
        longitude = String.valueOf(lon);
        System.out.println("Your Location: " + "\n" + "Latitude: " + latitude + "\n" + "Longitude: " + longitude);
        searchNearestPlace(voice2text);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void searchNearestPlace(String v2txt) {
        //.....
    }

















//    private boolean getLocation() {
////        latitude = "0";
////        longitude = "0";
//
//        if (checkbox.isChecked()) {
//            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                    != PackageManager.PERMISSION_GRANTED
//                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//                    != PackageManager.PERMISSION_GRANTED) {
//                // Request permission to access the user's location
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
////            System.out.println("Could not get permissions for location");
//                return false;
//            }
//            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//            double lon = location.getLongitude();
//            double lat = location.getLatitude();
//            latitude = String.valueOf(lat);
//            longitude = String.valueOf(lon);
//        }
//
//
//
//
//        else {
//
//            latitude = latInput.getText().toString();
//            longitude = lonInput.getText().toString();
//
//            if (latitude.isEmpty() || longitude.isEmpty()) {
////            System.out.println("The String is empty");
//
//                new Handler(Looper.getMainLooper()).post(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast toast = Toast.makeText(getApplicationContext(),
//                                "Please Enter Latitude and Longitude!",
//                                Toast.LENGTH_LONG);
//                        toast.show();
//                    }
//                });
//
//                return false;
//            }
//
//        }
//
//        System.out.println("Your Location: " + "\n" + "Latitude: " + latitude + "\n" + "Longitude: " + longitude);
//
//        return true;
//    }



}