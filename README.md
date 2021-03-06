# LightSwitch

This is the main module of the Lights application. It runs as a daemon on the server. It handles the automatic switching of the lights.
For an overview please see repository Lights.

The project structure is that of NetBeans 8.2.

History:

Version 1.1 - 07-06-2021
  -   Enhanced LightSensor. Instead of just counting the cycles needed to reach 'High' on the sensorpin the elapsed time (in hundredth if a millisecond) is returned. The OpenJDK runtime changes the algorithm used during execution (it gets much more efficient) so 'number of iterations' is no longer comparable.
  -   Deleted obsolete support for FM switches.
  -   Uses upgraded LightSupport.jar (Version 1.1)

Version 1.0.1 - 14-01-2019
  -   Logmessage for Time lights off enhanced
  -   Bug fixed in LightOffPeriod (used Settings.xPeriod instead of Settings.LightOffPeriod)

Version 1.0 - 04-12-2018
  -   English translation of LichtSchakel
  -   Changed to English protocol for esp switches
  -   Uses package jb.light.support
  -   Stopped using packages jb.licht.klassen and jb.licht.gegevens
