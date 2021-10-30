package com.example.weathernotifier.API;

// A list of this object is contained inside of WxOneCall. The list contains information about
// several hours over the next few days including what temperature it will likely be on the hour.

public class Hourly {

    // Bear in mind that for each dt in this API, it is in unix format by default
    public int dt;
    public float temp;
    public float feels_like;
    public float wind_deg;
    public Hourly() {}

}