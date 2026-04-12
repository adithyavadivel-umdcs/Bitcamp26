package com.example.bitcamp26.core.util;

/**
 * Geographic utility methods used by the HideNSeek game.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private GeoUtils() {
        // Utility class
    }

    /**
     * Returns the distance in meters between two latitude/longitude points
     * using the Haversine formula.
     */
    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLngRad = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaLatRad / 2.0) * Math.sin(deltaLatRad / 2.0)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLngRad / 2.0) * Math.sin(deltaLngRad / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Returns true if the second point lies within the given square half-width of the center
     * point. The historical method name is preserved for compatibility with existing callers.
     */
    public static boolean isWithinRadius(
            double centerLat,
            double centerLng,
            double targetLat,
            double targetLng,
            double radiusMeters
    ) {
        if (radiusMeters < 0) {
            return false;
        }

        return latitudeDeltaMeters(centerLat, targetLat) <= radiusMeters
                && longitudeDeltaMeters(centerLat, centerLng, targetLng) <= radiusMeters;
    }

    private static double latitudeDeltaMeters(double aLat, double bLat) {
        return Math.abs(bLat - aLat) * 111111.0;
    }

    private static double longitudeDeltaMeters(double centerLat,
                                               double centerLng,
                                               double targetLng) {
        double metersPerDegree = 111111.0
                * Math.max(Math.cos(Math.toRadians(centerLat)), 0.000001);
        double delta = normalizeLongitude(targetLng - centerLng);
        return Math.abs(delta) * metersPerDegree;
    }

    /**
     * Clamps a latitude value to the valid Earth range.
     */
    public static double clampLatitude(double latitude) {
        if (latitude > 90.0) {
            return 90.0;
        }
        if (latitude < -90.0) {
            return -90.0;
        }
        return latitude;
    }

    /**
     * Normalizes a longitude value into the range [-180, 180].
     */
    public static double normalizeLongitude(double longitude) {
        double normalized = longitude;

        while (normalized > 180.0) {
            normalized -= 360.0;
        }

        while (normalized < -180.0) {
            normalized += 360.0;
        }

        if (normalized == 180.0) {
            return -180.0;
        }

        return normalized;
    }

    /**
     * Returns true if the given latitude and longitude are both valid.
     */
    public static boolean isValidCoordinate(double latitude, double longitude) {
        return latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    /**
     * Returns the midpoint latitude between two coordinates.
     */
    public static double midpointLatitude(double lat1, double lat2) {
        return (lat1 + lat2) / 2.0;
    }

    /**
     * Returns the midpoint longitude between two coordinates.
     */
    public static double midpointLongitude(double lng1, double lng2) {
        double normalizedLng1 = normalizeLongitude(lng1);
        double normalizedLng2 = normalizeLongitude(lng2);
        double delta = normalizedLng2 - normalizedLng1;

        // Cross-dateline pairs should be averaged along the wrapped shortest arc.
        if (delta > 180.0) {
            normalizedLng1 += 360.0;
        } else if (delta < -180.0) {
            normalizedLng2 += 360.0;
        }

        return normalizeLongitude((normalizedLng1 + normalizedLng2) / 2.0);
    }
}
