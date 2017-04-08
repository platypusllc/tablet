# Platypus Control App  for Android
[![Build Status](https://travis-ci.org/platypusllc/tablet.svg?branch=master)](https://travis-ci.org/platypusllc/tablet)

This code is used for the tablet control interface, user can operate the boat either by teleoperation or by sending GPS waypoints.  There are three kinds of sensor data and battery voltage shown on the tablet. 

### Features ###
* Based on Shantanu's Mapbox branch.
* Supports a joystick or gamepad to control the boat.
* Adds offline map functionality.

----

Code in this repository is automatically built by Travis-CI. Any tags of this repostory will be automatically compiled to an APK and uploaded to both named Github Releases and the alpha channel on the Google Play store.

In order to do signed builds, the following environment variables must be set:

```
ANDROID_PLAY_JSON_FILE
ANDROID_RELEASE_STORE_FILE
ANDROID_RELEASE_STORE_PASSWORD
ANDROID_RELEASE_KEY_ALIAS
ANDROID_RELEASE_KEY_PASSWORD
MAPBOX_ACCESS_TOKEN
```

This deployment is based on the following setups:

https://github.com/codepath/android_guides/wiki/Automating-Publishing-to-the-Play-Store
https://github.com/larsgrefer/bpm-meter-android/blob/master/.travis.yml
https://github.com/Triple-T/gradle-play-publisher
