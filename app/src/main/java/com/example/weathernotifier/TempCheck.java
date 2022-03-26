package com.example.weathernotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.weathernotifier.API.Key;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;



// From Tutorial: https://stackoverflow.com/questions/34573109/how-to-make-an-android-app-to-always-run-in-background
// Better YouTube one : https://www.youtube.com/watch?v=bA7v1Ubjlzw

public class TempCheck extends Service {
    private Timer mTimer1;
    private TimerTask mTt1;
    private Handler mTimerHandler = new Handler();

    public String latitude = Key.getLat();
    public String longitude = Key.getLon();
    WxOneCall wx;
    float threshold = 40.7F;
    String tempUnits;
    int threshOption;
    public ArrayList<TempAlert> tempAlert;
    private Context thisContext;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        // do your jobs here
        thisContext = this;

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {

                        if(tempAlert == null)
                        {
                            tempAlert = new ArrayList<TempAlert>();
                        }

                        // Create a new temp Alert and add it to our list of them
                        // This is so we can have multiple of them going if we want
                        TempAlert alert = new TempAlert(intent, thisContext);

                        // TODO Might need to create an arrayList of handlers to make this work
                        startTimer(alert);

                        tempAlert.add(alert);

                        System.out.println("Number of Alerts now: " + tempAlert.size());

                    }
                }
        ).start();

        final String CHANNELID = "Foreground Service ID";
        NotificationChannel channel = new NotificationChannel(
                CHANNELID,
                CHANNELID,
                NotificationManager.IMPORTANCE_LOW
        );

        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification.Builder notification = new Notification.Builder(this, CHANNELID)
                .setContentText("You'll be notified when your threshold is reached")
                .setContentTitle("Watching the Temperature")
                .setSmallIcon(R.drawable.ic_baseline_add_alert_24);


        startForeground(1001,notification.build());

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        //  TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }



    private void startTimer(TempAlert alert){
        alert.mTimer1 = new Timer();
        alert.mTt1 = new TimerTask() {
            public void run() {
                mTimerHandler.post(() -> {
                    System.out.println("Time's up, foolish mortal");
                    new Thread(
                            () -> {
                                alert.tempDetect();
                                startTimer(alert);
                            }
                    ).start();
                });
            }
        };

        alert.mTimer1.schedule(alert.mTt1, alert.setTime);
    }

}

