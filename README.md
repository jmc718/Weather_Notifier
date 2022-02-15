# Overview

Note: This app is not finished yet. As of now, it's just a proof of concept.

I did a bit of digging around to see if an app like this exists. I haven't yet seen anything, so I'm making it.

This app will allow the user to set their phone to notify them when the temperature reaches a certain threshold. I was inspired to make this because during college I've lived in apartments without much air conditioning. When it's cool outside you want to keep the window open but you want to close it before it gets too hot.

Those who want to demo this app will need a java class called "Key" that contains their API key from Open Weather Map as well as their latitude and longitude. I plan to eventually include location functionality that will make it so that the latter two will not need to be hardcoded.


[Software Demo Video](https://youtu.be/Lx3kCP7myoY)

# Development Environment

I used Android Studio and Java to create this app. Open Weather Map's "One Call API" was also used.

# Useful Websites

{Make a list of websites that you found helpful in this project}
* [Open Weather Map](https://openweathermap.org/api)
* [YouTube Tutorial for Android Notifications](https://www.youtube.com/watch?v=Y73r1Q7RZwM)
* [JSON Formatter](https://jsonformatter.curiousconcept.com/#)
* [How to get Location Android](https://stackoverflow.com/questions/1513485/how-do-i-get-the-current-gps-location-programmatically-in-android)
* [ADB Connection Error Fix] (https://stackoverflow.com/questions/49340436/error-initializing-adb-unable-to-create-debug-bridge-unable-to-start-adb-serve)

# Future Work
* Timer functionality should be included to gauge when the app should next check the current temperature
* The app will use the hourly temperatures list to create a timer of when to check if the temperature has reached the threshold
* Functionality will be added so the app can run in the background