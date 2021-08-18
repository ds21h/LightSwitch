/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jb.light.switch_;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Sunrise /Sunset calculation
 *
 * Algorithm borrowed from
 * https://edwilliams.org/sunrise_sunset_algorithm.htm
 *
 */
public class SunriseSunset {
    static final double cDegreesToRadials = Math.PI / 180;
    static final double cRadialsToDegrees = 180 / Math.PI;

    /**
     * Calculates sunset for today on a given location.
     *
     * @param pLatitude North positive, South negative
     * @param pLongitude East positive, West negative
     * @return UTC date/time of sunset
     */
    public OffsetDateTime xSunset(double pLatitude, double pLongitude) {
        LocalDate lDate;
        int lMonth;
        int lDay;
        int lYearDay;
        int lYear;
        double lLongitudeHour;
        double lGlobal;
        double lMeanAnomaly;
        double lRealLongitude;
        double lRightAscension;
        double lRealLongitudeQuadrant;
        double lRightAscensionQuadrant;
        double lSinDeclination;
        double lCosDeclination;
        double lCosHour;
        double lLocalHourAngle;
        double lLocalMeanTime;
        double lUtcTime;
        int lTimeSec;
        OffsetDateTime lOffset;
        
        lDate = LocalDate.now();
        lMonth = lDate.getMonthValue();
        lDay = lDate.getDayOfMonth();
        lYearDay = lDate.getDayOfYear();
        lYear = lDate.getYear();

        // Transform Longitude into Hour-value 
        lLongitudeHour = pLongitude / 15;
        lGlobal = lYearDay + ((18 - lLongitudeHour) / 24);

        // Calculate mean anomality of the sun
        lMeanAnomaly = (0.9856 * lGlobal) - 3.289;

        // Calculate real longitude of the sun
        lRealLongitude = (lMeanAnomaly
                + (1.916 * Math.sin(lMeanAnomaly * cDegreesToRadials))
                + (0.020 * Math.sin(lMeanAnomaly * 2 * cDegreesToRadials))
                + 282.634);
        if (lRealLongitude < 0.0) {
            lRealLongitude += 360;
        }
        if (lRealLongitude > 360) {
            lRealLongitude -= 360;
        }

        // Calculate right ascension
        lRightAscension = cRadialsToDegrees * Math.atan(0.91764 * Math.tan(cDegreesToRadials * lRealLongitude));

        // Right ascension must be in the same quadrant as real longitude
        lRealLongitudeQuadrant = (Math.floor(lRealLongitude / 90) * 90);
        lRightAscensionQuadrant = (Math.floor(lRightAscension / 90) * 90);
        lRightAscension = lRightAscension + (lRealLongitudeQuadrant - lRightAscensionQuadrant);

        // Translate right ascension to hours
        lRightAscension = lRightAscension / 15;

        // Calculate declination of the sun
        lSinDeclination = (0.39782 * (Math.sin(cDegreesToRadials * lRealLongitude)));
        lCosDeclination = Math.cos(Math.asin(lSinDeclination));

        // Calulate local hour angle. Use offial zenith (90gr 50') ==> cos(zenith) = -0.01454
        lCosHour = ((-0.01454) - (lSinDeclination * (Math.sin(cDegreesToRadials * pLatitude)))) / (lCosDeclination * Math.cos(cDegreesToRadials * pLatitude));
        if (lCosHour < -1) {
            // The sun does not set!
            return null;
        }

        // Finish Hour angle calcutation and convert it to hours
        lLocalHourAngle = (cRadialsToDegrees * Math.acos(lCosHour)) / 15;

        // Calculate local (geographic) time of sunset.
        lLocalMeanTime = lLocalHourAngle + lRightAscension - (0.06571 * lGlobal) - 6.622;

        // Transform to UTC
        lUtcTime = lLocalMeanTime - lLongitudeHour;
        if (lUtcTime < 0) {
            lUtcTime += 24;
        }

        // Transform to OffsetDateTime format
        lTimeSec = (int) Math.floor(lUtcTime * 60 * 60);
        lOffset = OffsetDateTime.of(lYear, lMonth, lDay, 0, 0, 0, 0, ZoneOffset.UTC).plusSeconds(lTimeSec);

        return lOffset;
    }
}
