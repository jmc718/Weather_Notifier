package com.example.weathernotifier;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.weathernotifier.API.Key;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";
    private static final int REQUEST_LOCATION = 1;
    public String latitude;
    public String longitude;
    WxOneCall wx;
    float threshold = 40.7F;

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

                        tempDetect();

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
        //  TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    /*
    Function: Temp Detect
    Description: This is the meat and potatoes of the whole app. This will check the API, wait
    until the right time, then send the user a notification that their threshold has been reached.
     */
    private void tempDetect() {
//      Access the API and store it in a class
        wx = getTempInfo();
        int start = 0;

//        if the temperature has already breached the threshold
        if (wx.current.temp > threshold){
//        send an alert
            sendNotification();
//        check what time the temperature will no longer be breached
            int iBelow = findNextBelow(wx, threshold, 0);
//        if there are none, set a timer for a day or so
            if (iBelow > wx.hourly.size()) {
                System.out.println("Setting timer for ");
                displayDateTime(wx.hourly.get(24).dt);
                Date date = convertDateTime(wx.hourly.get(24).dt);
                startTimer(date);
                return;
            }
//            otherwise, use it as the start of our next check:
//            when will the temperature once again be above the threshold?
            else {
                start = iBelow;
            }

        }

//        check what hour will reach established threshold parameters
        int iAbove = findNextAbove(wx,threshold,start);

        // Initialize target date
        Date tarDate;

//        if there are no times that will reach the threshold
        if (iAbove > wx.hourly.size()) {
//        set a timer to check again after a day or so
            System.out.println("Setting timer for ");
            displayDateTime(wx.hourly.get(24).dt);
            tarDate = convertDateTime(wx.hourly.get(24).dt);
            startTimer(tarDate);
            return;
        }

        // else if the temperature will be reached within the hour

        else if (iAbove == 0) {
            System.out.println("The temperature will be reached within the hour.");
            // Get the target date time and the current date time
            tarDate = convertDateTime(wx.hourly.get(24).dt);
            Date currentDate = convertDateTime(wx.current.dt);
            // Let's grab the current minute
            int currentMinute = getDateMinutes(currentDate);

            // if there are more than 5 minutes between current minute and the hour
            if ((60 - currentMinute) > 5 ) {
                System.out.println("There are more than 5 minutes until the hour.");

                // find, percentage wise, where the threshold lies between the current temperature
                // and the temperature at the hour
                float percentDistance = (threshold - wx.current.temp)
                        / (wx.hourly.get(0).temp - wx.current.temp);

                // set the minute based on an approximation of when the temperature might be reached
                // this is based on the percent of the distance between the current temperature to
                // the threshold to the next hour's temperature
                currentMinute += Math.round(percentDistance * (60 - currentMinute));


                // set the time for halfway between the two (old algorithm)
//                currentMinute += ((60 - currentMinute) / 2);
                currentDate = setDateMinutes(currentDate, currentMinute);
                // the "currentDate" is no longer the current date!
                // rather, it is a date value we're going to use to...

                // set the timer to go off at that time.
                System.out.println("Setting timer for ");
                displayDateTime(currentDate);
                startTimer(currentDate);
                // I could create a separate object like "newDate" or something, but why waste data?

            }

            // otherwise, we might as well just wait it out until the hour arrives
            else {
                System.out.println("There are 5 minutes or less until the hour.");
                System.out.println("Setting timer for ");
                displayDateTime(tarDate);
                startTimer(tarDate);

            }

        }

//        otherwise, set a timer to check again one hour early
        else {
            System.out.println("Setting timer for ");
            displayDateTime(wx.hourly.get(iAbove - 1).dt);
            tarDate = convertDateTime(wx.hourly.get(iAbove - 1).dt);
            startTimer(tarDate);
            return;
        }



//        if a timer is set, the function should return and wait for the timer to finish
//        and call the function "recursively"

    }

    private Date setDateMinutes(Date date, int minute) {
        //create a new calendar object
        Calendar cal = Calendar.getInstance();
        // set the calendar's time to the date's time
        cal.setTime(date);
        // set the calendar's minute to the desired minute
        cal.set(Calendar.MINUTE, minute);
        // return date object with new minute
        return cal.getTime();
    }


    private int getDateMinutes(Date date) {
        //create a new calendar object
        Calendar cal = Calendar.getInstance();
        // set the calendar's time to the date's time
        cal.setTime(date);
        // return minute as int
        return cal.get(cal.MINUTE);
    }

    // This was some code I found online for displyaing a unix date time in a more readable format
    public void displayDateTime(int unix_seconds) {
        //Unix seconds
        //convert seconds to milliseconds
        Date date = new Date(unix_seconds * 1000L);
        // format of the date
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        jdf.setTimeZone(TimeZone.getTimeZone("GMT-7"));
        String java_date = jdf.format(date);
        System.out.println(java_date);
    }

    // Overloaded function that uses Date instead
    public void displayDateTime(Date date) {
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        jdf.setTimeZone(TimeZone.getTimeZone("GMT-7"));
        String java_date = jdf.format(date);
        System.out.println(java_date);
    }

    public Date convertDateTime(int unix_seconds) {
        return new Date(unix_seconds * 1000L);
    }


    private void stopTimer(){
        if(mTimer1 != null){
            mTimer1.cancel();
            mTimer1.purge();
        }
    }

    private void startTimer(Date date){
        mTimer1 = new Timer();
        mTt1 = new TimerTask() {
            public void run() {
                mTimerHandler.post(new Runnable() {
                    public void run(){
                        System.out.println("Time's up, foolish mortal");
                        new Thread(
                                () -> tempDetect()
                        ).start();
                    }
                });
            }
        };

        mTimer1.schedule(mTt1, date);
    }

    private WxOneCall getTempInfo() {
        Gson gson = new Gson();
        // Thanks Brother Macbeth for the awesome ability to easily pull out the JSON as a string
        HTTPHelper http = new HTTPHelper();
        // The private key and my location won't be shared publicly in the git repo
        System.out.println("https://api.openweathermap.org/data/2.5/onecall?lat="
                + Key.getLat() + "&lon=" + Key.getLon() + "&appid=" + Key.getKey() +
                "&units=imperial");
        String result = http.readHTTP("https://api.openweathermap.org/data/2.5/onecall?lat="
                + Key.getLat() + "&lon=" + Key.getLon() + "&appid=" + Key.getKey() +
                "&units=imperial");

        // The classes should be structured correctly
        WxOneCall wx = gson.fromJson(result, WxOneCall.class);

        Date curr = convertDateTime(wx.current.dt);
        Date firstHour = convertDateTime(wx.hourly.get(0).dt);

        // We don't care about stuff that's already happened.
        // if the first hour index happened before now, remove it.
        if (curr.after(firstHour)) {
            wx.hourly.remove(0);
        }

        System.out.println("Current Time: ");
        displayDateTime(wx.current.dt);

        System.out.println("Time at index 0: ");
        displayDateTime(wx.hourly.get(0).dt);
        System.out.println("Temp at index 0: " + wx.hourly.get(0).temp);

        // Mostly to test out that we've got our API working, let's print the current temp
        System.out.println("The Current Temperature is " + wx.current.temp + "°F");

        return wx;
    }

    // When we want to send a notification to the user that it's getting hot and should
    // close the window
    public void sendNotification() {
        // build the notification itself so it can be sent off later in the function
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "hot")
                .setSmallIcon(R.drawable.ic_baseline_add_alert_24)
                .setContentTitle("It's getting hot in here")
                .setContentText("You should probably close the window")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Android has some special weird stuff it has to do to make this work
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Do the actual sending of the notification
        notificationManager.notify(100, builder.build());


    }

    /*
    Function: Find Next Below
    Description: Finds the index of the next time the temperature will be below the given threshold
     */
    private int findNextBelow(WxOneCall wx, float threshold, int start) {
        int i = start;
        while (i < wx.hourly.size()) {
            // check if the hour's expected temperature is below the threshold
            if (wx.hourly.get(i).temp < threshold) {
                // and if it is, display the hour's date and time
                displayDateTime(wx.hourly.get(i).dt);
                // as well as the expected temperature
                System.out.println("The Temperature will be " + wx.hourly.get(i).temp + "°F"
                        + " Which is below our threshold of " + threshold);
                // return the index
                return i;

            }
            i++;
        }
        System.out.println("No temp below threshold found");
        return wx.hourly.size() + 1;
    }

    /*
    Function: Find Next Above
    Description: Finds the index of the next time the temperature will be above the given threshold
     */
    private int findNextAbove(WxOneCall wx, float threshold, int start) {
        int i = start;
        while (i < wx.hourly.size()) {
            // check if the hour's expected temperature is above the threshold
            if (wx.hourly.get(i).temp > threshold) {
                // and if it is, display the hour's date and time
                displayDateTime(wx.hourly.get(i).dt);
                // as well as the expected temperature
                System.out.println("The Temperature will be " + wx.hourly.get(i).temp + "°F"
                        + " Which is above our threshold of " + threshold);
                // return the index
                return i;

            }
            i++;
        }
        System.out.println("No temp above threshold found");
        return wx.hourly.size() + 1;
    }


}

