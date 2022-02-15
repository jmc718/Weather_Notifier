package com.example.weathernotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;



// From Tutorial: https://stackoverflow.com/questions/34573109/how-to-make-an-android-app-to-always-run-in-background
// Better YouTube one : https://www.youtube.com/watch?v=bA7v1Ubjlzw

public class TempCheck extends Service {
    private Timer mTimer1;
    private TimerTask mTt1;
    private Handler mTimerHandler = new Handler();

    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";

    public TempCheck() {
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        // do your jobs here

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {

                        startTimer();


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
                .setContentText("Service is running")
                .setContentTitle("Service enabled")
                .setSmallIcon(R.drawable.ic_baseline_add_alert_24);


        startForeground(1001,notification.build());

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

//    private void startForeground() {
//        Intent notificationIntent = new Intent(this, MainActivity.class);
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
//                notificationIntent, 0);
//
//        startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
//                NOTIF_CHANNEL_ID) // don't forget create a notification channel first
//                .setOngoing(true)
//                .setSmallIcon(R.drawable.ic_baseline_add_alert_24)
//                .setContentTitle(getString(R.string.app_name))
//                .setContentText("Service is running background")
//                .setContentIntent(pendingIntent)
//                .build());
//    }

    private void stopTimer(){
        if(mTimer1 != null){
            mTimer1.cancel();
            mTimer1.purge();
        }
    }

    private void startTimer(){
        mTimer1 = new Timer();
        mTt1 = new TimerTask() {
            public void run() {
                mTimerHandler.post(new Runnable() {
                    public void run(){
                        System.out.println("Time's up, foolish mortal");
                    }
                });
            }
        };

        mTimer1.schedule(mTt1, 15000);
    }


}

