package com.example.weathernotifier.API;


// This is inside WxOneCall. It contains current weather information. Like what the temperature
// is right now.

public class Current {

    // Bear in mind that for each dt in this API, it is in unix format by default
    public int dt;
    public float temp;
    public float feels_like;
    public float wind_deg;
    public Current() {}

}