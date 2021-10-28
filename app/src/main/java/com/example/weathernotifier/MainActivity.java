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
        HTTPHelper http = new HTTPHelper();
        String result = http.readHTTP("https://api.openweathermap.org/data/2.5/onecall?lat="
                + Key.getLat() + "&lon=" + Key.getLon() + "&appid=" + Key.getKey() +
                "&units=imperial");
        WxOneCall wx = gson.fromJson(result, WxOneCall.class);
        System.out.println("The Current Temperature is " + wx.current.temp + "°F");

//        System.out.println(wx.current.dt);
//        displayDateTime(wx.current.dt);


        String cheese = threshInput.getText().toString();

        threshold = Float.parseFloat(cheese);

        int i = 0;
        while (i < wx.hourly.size()) {
            if (wx.hourly.get(i).temp > threshold){
                displayDateTime(wx.hourly.get(i).dt);
                System.out.println("The Temperature will be " + wx.hourly.get(i).temp + "°F");
            }
            i++;
        }

    }


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

    public void sendNotification(View view) {

        System.out.println("In sendNotification");


        Notification notification = new Notification.Builder(MainActivity.this)
                .setContentTitle("It's getting hot in here")
                .setContentText("You should probably close the window")
                .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "hot")
                .setSmallIcon(R.drawable.ic_baseline_add_alert_24)
                .setContentTitle("It's getting hot in here")
                .setContentText("You should probably close the window")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        notificationManager.notify(100,builder.build());


    }

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