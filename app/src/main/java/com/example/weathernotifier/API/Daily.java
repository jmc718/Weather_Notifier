package com.example.weathernotifier.API;

// A list of this object is contained inside of WxOneCall. It may or may not be important for
// this app

public class Daily {

    // Bear in mind that for each dt in this API, it is in unix format by default
    public int dt;
    public int sunrise;
    public int sunset;
    public Daily() {}

}