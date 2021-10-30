package com.example.weathernotifier;

import com.example.weathernotifier.API.Current;
import com.example.weathernotifier.API.Daily;
import com.example.weathernotifier.API.Hourly;

import java.util.List;

// This is the top of the API. Everything else is contained herein.

public class WxOneCall {

    public String timezone;
    public Current current;
    public List<Hourly> hourly;
    public List<Daily> daily;
    public WxOneCall() {}

}
