package com.yeonfish.sharelocation.util;

public class SpeedUtil {
    // Calculate distance
    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Radius of the earth in km
        double dLat = deg2rad(lat2-lat1);
        double dLon = deg2rad(lon2-lon1);
        double a =
                Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                                Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c; // Distance in km
    }

    // Calculate speed by times and locations
    public static double calculateSpeed(long t1, double lat1, double lon1, long t2, double lat2, double lon2) {
        return ((getDistance(lat1, lon1, lat2, lon2)/((t2-t1)/1000))*60*60);
    }

    // Convert degrees to radians
    private static double deg2rad(double deg) {
        return deg * (Math.PI/180);
    }
}
