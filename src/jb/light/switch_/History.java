package jb.light.switch_;

/*
Version 1.0 - 04-12-2018
  -   English translation of LichtSchakel
  -   Changed to English protocol for esp switches
  -   Uses package jb.light.support
  -   Stopped using packages jb.licht.klassen and jb.licht.gegevens
Version 1.0.1 - 14-01-2019
  -   Logmessage for Time lights off enhanced
  -   Bug fixed in LightOffPeriod (used Settings.xPeriod instead of Settings.LightOffPeriod)
Version 1.1 - 03-06-2021
  -   Uses upgraded LightSupport.jar (Version 1.1)
  -   Enhanced LightSensor. Instead of just counting the cycles needed to reach 'High' on the sensorpin the elapsed time (in hundredth if a millisecond) is returned. The OpenJDK runtime changes the algorithm used during execution (it gets much more efficient) so 'number of iterations' is no longer comparable.
*/
