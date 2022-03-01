package com.example.weathernotifier;

import android.app.Application;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

/*
Got this from:
https://stackoverflow.com/questions/19565685/saving-logcat-to-a-text-file-in-android-device


In case this doesn't work out, here's how to append to an external file:
https://stackoverflow.com/questions/1625234/how-to-append-text-to-an-existing-file-in-java


 */



public class MyPersonalApp extends Application {

    /**
     * Called when the application is starting, before any activity, service, or receiver objects (excluding content providers) have been created.
     */
    public void onCreate() {
        super.onCreate();

        if ( isExternalStorageWritable() ) {

            System.out.println("External Storage is indeed writable");

            File appDirectory = new File( Environment.getExternalStorageDirectory() + "/MyPersonalAppFolder" );
            File logDirectory = new File( appDirectory + "/logs" );
            File logFile = new File( logDirectory, "logcat_" + System.currentTimeMillis() + ".txt" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec("logcat -c");
                process = Runtime.getRuntime().exec("logcat -f " + logFile);
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        } else if ( isExternalStorageReadable() ) {
            // only readable
        } else {
            // not accessible
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }
}
