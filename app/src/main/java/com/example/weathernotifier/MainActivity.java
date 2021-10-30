package com.example.weathernotifier;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;


import com.example.weathernotifier.API.Key;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {

    Float threshold;

    EditText threshInput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        threshInput = (EditText) findViewById(R.id.tempThreshold);

    }


    /** Called when the user touches the button */
    public void sendMessage(View view) {

        // We need this to be on a thread lest the app freak out and think that it is frozen
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
               checkTempsThread();
            }
        });
        t.start();
    }


    public void checkTempsThread() {
        Gson gson = new Gson();
        // Thanks Brother Macbeth for the awesome ability to easily pull out the JSON as a string
        HTTPHelper http = new HTTPHelper();
        // The private key and my location won't be shared publicly in the git repo
        String result = http.readHTTP("https://api.openweathermap.org/data/2.5/onecall?lat="
                + Key.getLat() + "&lon=" + Key.getLon() + "&appid=" + Key.getKey() +
                "&units=imperial");
        // The classes should be structured correctly
        WxOneCall wx = gson.fromJson(result, WxOneCall.class);

        // Mostly to test out that we've got our API working, let's print the current temp
        System.out.println("The Current Temperature is " + wx.current.temp + "°F");

        // Get the threshold out of the editable text box
        String cheese = threshInput.getText().toString();
        // now take the string and put it in the threshold variable as a float
        threshold = Float.parseFloat(cheese);

        // The Hourly class contains temperature information about every hour over the next few days
        // For testing purposes for now, let's loop through every hour and check the temperature
        // to see if it will be above the threshold
        int i = 0;
        // while there are still more hours in the list
        while (i < wx.hourly.size()) {
            // check if the hour's expected temperature is above the threshold
            if (wx.hourly.get(i).temp > threshold){
                // and if it is, display the hour's date and time
                displayDateTime(wx.hourly.get(i).dt);
                // as well as the expected temperature
                System.out.println("The Temperature will be " + wx.hourly.get(i).temp + "°F");
            }
            i++;
        }

    }

    // This was some code I found online for displyaing a unix date time in a more readable format
    public void displayDateTime(int unix_seconds) {
        //Unix seconds
        //convert seconds to milliseconds
        Date date = new Date(unix_seconds*1000L);
        // format of the date
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        jdf.setTimeZone(TimeZone.getTimeZone("GMT-6"));
        String java_date = jdf.format(date);
        System.out.println("\n"+java_date+"\n");
    }

    // When we want to send a notification to the user that it's getting hot and should
    // close the window
    public void sendNotification(View view) {
        // build the notification itself so it can be sent off later in the function
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "hot")
                .setSmallIcon(R.drawable.ic_baseline_add_alert_24)
                .setContentTitle("It's getting hot in here")
                .setContentText("You should probably close the window")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Android has some special weird stuff it has to do to make this work
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Do the actual sending of the notification
        notificationManager.notify(100,builder.build());


    }

    // A hard coded (for now at least) notification channel through which the app can send the
    // notification
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "hotChannel";
            String description = "Channel for letting the user know it is getting hot";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("hot", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }



}