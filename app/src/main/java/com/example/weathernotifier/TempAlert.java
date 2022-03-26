package com.example.weathernotifier;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.weathernotifier.API.Key;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class TempAlert {
    public Timer mTimer1;
    public TimerTask mTt1;

    public String latitude;
    public String longitude;
    public WxOneCall wx;
    public float threshold = 40.7F;
    public String tempUnits;
    public String tempDeg;
    public int threshOption;
    private Context thisContext;
    public Date setTime;

    TempAlert(Intent intent, Context context) {
        threshold = intent.getFloatExtra("threshold",threshold);
        // Code number for above or below. Above by default.
        threshOption = intent.getIntExtra("threshOption",1);

        thisContext = context;
        // Get GPS Coords from MainActivity. Comment these two lines to use the ones
        // in the Key Class
        latitude = intent.getStringExtra("lat");
        longitude = intent.getStringExtra("lon");
        // Is it Latitude or Longitude?
        tempUnits = intent.getStringExtra("tempUnits");

        System.out.println("temp units is " + tempUnits);

        if (tempUnits.equals("imperial")) {
            tempDeg = "°F";
        }

        else if (tempUnits.equals("metric")) {
            tempDeg = "°C";
        }

        else {
            tempDeg = "°";
        }

        System.out.println("temp deg is " + tempDeg);

        tempDetect();

    }
    /*
    Function: Temp Detect
    Description: This is the meat and potatoes of the whole app. This will check the API, wait
    until the right time, then send the user a notification that their threshold has been reached.
     */
    public void tempDetect() {

        if (threshOption == 1)
            detectAbove();

        if (threshOption == 2)
            detectBelow();

    }




    private void detectAbove() {

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
                displayDateTime(wx.hourly.get(24).dt, wx.timezone);
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
        // within a reasonable timeframe
        if (iAbove > 16) {
//        set a timer to check again after a reasonable time
            System.out.println("Setting timer for ");
            displayDateTime(wx.hourly.get(12).dt, wx.timezone);
            tarDate = convertDateTime(wx.hourly.get(12).dt);
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
                displayDateTime(currentDate, wx.timezone);
                startTimer(currentDate);
                // I could create a separate object like "newDate" or something, but why waste data?

            }

            // otherwise, we might as well just wait it out until the hour arrives
            else {
                System.out.println("There are 5 minutes or less until the hour.");
                System.out.println("Setting timer for ");
                displayDateTime(tarDate, wx.timezone);
                startTimer(tarDate);

            }

        }



//        otherwise, set a timer to check again a little bit early
        else {
            // the further out it is, the more inaccurate the prediction is likely to be.
            // As such, we want to cut off about one quarter of the time and check then.
            int target = (int) Math.ceil(iAbove - (iAbove * .25));

            System.out.println("API says threshold will be reached " + iAbove
                    + " hours from now. Setting a timer for " + target
                    + " hours from now.");

            System.out.println("Setting timer for ");
            displayDateTime(wx.hourly.get(target).dt, wx.timezone);
            tarDate = convertDateTime(wx.hourly.get(target).dt);
            startTimer(tarDate);
            return;
        }



//        if a timer is set, the function should return and wait for the timer to finish
//        and call the function "recursively"

    }

    private void detectBelow() {

//      Access the API and store it in a class
        wx = getTempInfo();
        int start = 0;


//        if the temperature has already breached the threshold
        if (wx.current.temp < threshold){
//        send an alert
            sendNotification();
//        check what time the temperature will no longer be breached
            int iAbove = findNextAbove(wx, threshold, 0);
//        if there are none, set a timer for a day or so
            if (iAbove > wx.hourly.size()) {
                System.out.println("Setting timer for ");
                displayDateTime(wx.hourly.get(24).dt, wx.timezone);
                Date date = convertDateTime(wx.hourly.get(24).dt);
                startTimer(date);
                return;
            }
//            otherwise, use it as the start of our next check:
//            when will the temperature once again be below the threshold?
            else {
                start = iAbove;
            }

        }

//        check what hour will reach established threshold parameters
        int iBelow = findNextBelow(wx,threshold,start);

        // Initialize target date
        Date tarDate;

//        if there are no times that will reach the threshold
//        // within a reasonable timeframe
        if (iBelow > 16) {
//        set a timer to check again at a reasonable time
            System.out.println("Setting timer for ");
            displayDateTime(wx.hourly.get(12).dt, wx.timezone);
            tarDate = convertDateTime(wx.hourly.get(12).dt);
            startTimer(tarDate);
            return;
        }

        // else if the temperature will be reached within the hour

        else if (iBelow == 0) {
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
                float percentDistance = (wx.current.temp - threshold)
                        / (wx.current.temp - wx.hourly.get(0).temp);

                // set the minute based on an approximation of when the temperature might be reached
                // this is based on the percent of the distance between the current temperature to
                // the threshold to the next hour's temperature
                currentMinute += Math.round(percentDistance * (60 - currentMinute));

                currentDate = setDateMinutes(currentDate, currentMinute);
                // the "currentDate" is no longer the current date!
                // rather, it is a date value we're going to use to...

                // set the timer to go off at that time.
                System.out.println("Setting timer for ");
                displayDateTime(currentDate, wx.timezone);
                startTimer(currentDate);
                // I could create a separate object like "newDate" or something, but why waste data?

            }

            // otherwise, we might as well just wait it out until the hour arrives
            else {
                System.out.println("There are 5 minutes or less until the hour.");
                System.out.println("Setting timer for ");
                displayDateTime(tarDate, wx.timezone);
                startTimer(tarDate);

            }

        }



//        otherwise, set a timer to check again early
        else {
            // the further out it is, the more inaccurate the prediction is likely to be.
            // As such, we want to cut off about one quarter of the time and check then.
            int target = (int) Math.ceil(iBelow - (iBelow * .25));

            System.out.println("API says threshold will be reached " + iBelow
                    + " hours from now. Setting a timer for " + target
                    + " hours from now.");

            System.out.println("Setting timer for ");
            displayDateTime(wx.hourly.get(target).dt, wx.timezone);
            tarDate = convertDateTime(wx.hourly.get(target).dt);
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
    public void displayDateTime(int unix_seconds, String timezone) {
        //Unix seconds
        //convert seconds to milliseconds
        Date date = new Date(unix_seconds * 1000L);
        // format of the date
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        jdf.setTimeZone(TimeZone.getTimeZone(timezone));
        String java_date = jdf.format(date);
        System.out.println(java_date);
    }

    // Overloaded function that uses Date instead
    public void displayDateTime(Date date, String timezone) {
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        jdf.setTimeZone(TimeZone.getTimeZone(timezone));
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
        setTime = date;

    }

    private WxOneCall getTempInfo() {
        Gson gson = new Gson();
        // Thanks Brother Macbeth for the awesome ability to easily pull out the JSON as a string
        HTTPHelper http = new HTTPHelper();
        // The private key and my location won't be shared publicly in the git repo
        System.out.println("https://api.openweathermap.org/data/2.5/onecall?lat="
                + latitude + "&lon=" + longitude + "&appid=" + Key.getKey() +
                "&units=" + tempUnits);
        String result = http.readHTTP("https://api.openweathermap.org/data/2.5/onecall?lat="
                + latitude + "&lon=" + longitude + "&appid=" + Key.getKey() +
                "&units=" + tempUnits);

        // The classes should be structured correctly
        WxOneCall wx = gson.fromJson(result, WxOneCall.class);

        Date curr = convertDateTime(wx.current.dt);
        Date firstHour = convertDateTime(wx.hourly.get(0).dt);

        // We don't care about stuff that's already happened.
        // if the first hour index happened before now, remove it.
        while (curr.after(firstHour)) {
            wx.hourly.remove(0);
            firstHour = convertDateTime(wx.hourly.get(0).dt);
        }


        System.out.println("Time Zone is");
        System.out.println(wx.timezone);

        System.out.println("Current Time: ");
        displayDateTime(wx.current.dt, wx.timezone);

        System.out.println("Time at index 0: ");
        displayDateTime(wx.hourly.get(0).dt, wx.timezone);
        System.out.println("Temp at index 0: " + wx.hourly.get(0).temp);

        // Mostly to test out that we've got our API working, let's print the current temp
        System.out.println("The Current Temperature is " + wx.current.temp + tempDeg);

        return wx;
    }

    // When we want to send a notification to the user that it's getting hot and should
    // close the window
    public void sendNotification() {
        // build the notification itself so it can be sent off later in the function
        String title = "";
        String text = "";

        title += "Threshold reached!";
        text += "It is now ";

        if (threshOption == 1) {
            text += "above ";
        }
        else {
            text += "below ";
        }

        text += threshold + tempDeg;


        NotificationCompat.Builder builder = new NotificationCompat.Builder(thisContext, "temp")
                .setSmallIcon(R.drawable.ic_baseline_add_alert_24)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Android has some special weird stuff it has to do to make this work
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(thisContext);

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
                displayDateTime(wx.hourly.get(i).dt, wx.timezone);
                // as well as the expected temperature
                System.out.println("The Temperature will be " + wx.hourly.get(i).temp + tempDeg
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
                displayDateTime(wx.hourly.get(i).dt, wx.timezone);
                // as well as the expected temperature
                System.out.println("The Temperature will be " + wx.hourly.get(i).temp + tempDeg
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
