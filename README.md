# Running the Steam Example

## Quick Start
* Download and install [Android Studio](https://developer.android.com/sdk/index.html)
* When you run Android Studio and the "Welcome to Android Studio" window opens, select _"Import project"_, 
  if you already have a project open select _"New...>Import Project..."_
* If asked, select the "android-steam-thing" and Select "Gradle Project to Import" 
* If asked, use gradlew which is provided
* During this process you may be asked to install or choose an android SDK. This project targets API 19.
* Import the Entity file samples/android-steam-thing/entity/Things_SteamSensor1.xml into your ThingWorx 
  server. This will give you a SteamSensor1 Thing which this app updates.
* Your project should import cleanly and be ready to use. Choose Run 'Android-Steam-Thing' from the Run menu to start it.
* Choose the "Launch Emulator" option and use the "..." button. Then select the "Create Virtual Device Button"
  on the bottom of the next window. Choose a device from the list. If given the option, choose an x86 
  device over an Arm device to get better performance, If you don't have any x86 choices, consider 
  clicking on the show downloadable images button to find one.
* The Steam App should start in the simulator
* If the App is starting for the first time you will be taken to the settings page. You must provide:
    - A URI of the form ws://hostname:8080/Thingworx/WS for http access
    - A URI of the form wss://hostname:443/Thingworx/WS for https access
    - An appKey which can be generated on your ThingWorx server in the _Application Keys_ section.

## Project Details
* If you plan on building with gradle, use the provided gradlew or gradlew.bat files instead of the 
  gradle that may be installed on your system.
* If you do not have any SDK's installed use the Tools>Android>SDK Manager tools to download API 19 
  which is what this project is based on.
* If you have an Android phone and want to deploy to it, attach it to your computer using a USB cable. 
  The phone must be placed into developer mode. These instructions vary by phone. This page may provide 
  a good starting point for enabling developer mode. http://developer.android.com/tools/device.html
* If you do not have an Android device, Choose the "Launch Emulator" option and use the "..." button.
  Then select the "Create Virtual Device Button" on the bottom of the next window. Choose a device 
  from the list. If given the option, choose an x86 device over an Arm device to get better performance, 
  If you don't have any x86 choices, consider clicking on the show downloadable images button to find
  one. If you don't have an x86 option, use an Arm image.
* The log output level for the SDK ships set at TRACE. This will create multiple log entries in your
  *Android Monitor* tab. This is intended to help a new developer to get started but tThis output can
  be reduced by replacing the word TRACE with the word ERROR in the _samples/android-steam-thing/src/main/assets/logback.xml_ 
  configuration file.
