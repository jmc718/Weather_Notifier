package com.example.weathernotifier;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
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

public class MainActivity extends AppCompatActivity {

    Float threshold;

    EditText threshInput;

    private static final int REQUEST_LOCATION = 1;
    public String latitude;
    public String longitude;

    private Timer mTimer1;
    private TimerTask mTt1;
    private Handler mTimerHandler = new Handler();

    RadioGroup rgDegree;
    RadioGroup threshOption;
    RadioButton radioButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {


//      This is For Push notifications
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        threshInput = findViewById(R.id.tempThreshold);
        rgDegree = findViewById(R.id.tempMeasurement);
        threshOption = findViewById(R.id.threshOption);



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

        // This is to get the stuff inside the celsius/fahrenheit radio buttons
        int radioId = rgDegree.getCheckedRadioButtonId();
        radioButton = findViewById(radioId);

        System.out.println("Temperature will be measured in " + radioButton.getText());

        String tempUnits = "standard";

        if (radioButton.getText().charAt(1) == 'F')
            tempUnits = "imperial";
        if (radioButton.getText().charAt(1) == 'C')
            tempUnits = "metric";

        // Get the stuff inside the above/below radio buttons
        radioId = threshOption.getCheckedRadioButtonId();
        radioButton = findViewById(radioId);

        System.out.println("Checking for temps " + radioButton.getText());

        int threshNum = 1;

        if (radioButton.getText().charAt(0) == 'A')
            threshNum = 1;
        if (radioButton.getText().charAt(0) == 'B')
            threshNum = 2;


        // Get the GPS location from the user
        getLocation();

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

    private void getLocation() {
//        latitude = "0";
//        longitude = "0";
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission to access the user's location
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
//            System.out.println("Could not get permissions for location");
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double lon = location.getLongitude();
        double lat = location.getLatitude();
        latitude = String.valueOf(lat);
        longitude = String.valueOf(lon);
        System.out.println("Your Location: " + "\n" + "Latitude: " + latitude + "\n" + "Longitude: " + longitude);

    }


}