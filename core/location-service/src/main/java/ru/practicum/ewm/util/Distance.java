package ru.practicum.ewm.util;

import static java.lang.Math.*;

public class Distance {
    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        final int DEGREE_180 = 180;
        final double KOEF = 60 * 1.8524;
        double dist = 0;
        double radLat1;
        double radLat2;
        double theta;
        double radTheta;

        if (lat1 == lat2 && lon1 == lon2)
            return dist;

        radLat1 = PI * lat1 / DEGREE_180;
        radLat2 = PI * lat2 / DEGREE_180;
        theta = lon1 - lon2;
        radTheta = PI * theta / DEGREE_180;
        dist = sin(radLat1) * sin(radLat2) + cos(radLat1) * cos(radLat2) * cos(radTheta);

        dist = dist > 1 ? 1 : dist;
        dist = acos(dist);
        dist = dist * DEGREE_180 / PI;
        dist = dist * KOEF;

        return dist;
    }
}
