package com.example.bitcamp26.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GeoUtilsTest {

    @Test
    public void distanceMeters_handlesIdentityAndApproximateDistance() {
        assertEquals(0.0, GeoUtils.distanceMeters(0, 0, 0, 0), 0.0001);

        double distance = GeoUtils.distanceMeters(0, 0, 0.0009, 0);
        assertTrue(distance > 90);
        assertTrue(distance < 110);
    }

    @Test
    public void isWithinRadius_rejectsNegativeRadiusAndChecksDistance() {
        assertFalse(GeoUtils.isWithinRadius(0, 0, 0, 0, -1));
        assertTrue(GeoUtils.isWithinRadius(0, 0, 0.00005, 0, 10));
        assertTrue(GeoUtils.isWithinRadius(0, 0, 0.00007, 0.00007, 10));
        assertFalse(GeoUtils.isWithinRadius(0, 0, 0.001, 0, 10));
        assertFalse(GeoUtils.isWithinRadius(0, 0, 0.00011, 0, 10));
    }

    @Test
    public void clampLatitude_enforcesEarthBounds() {
        assertEquals(90.0, GeoUtils.clampLatitude(100.0), 0.0001);
        assertEquals(-90.0, GeoUtils.clampLatitude(-100.0), 0.0001);
        assertEquals(45.0, GeoUtils.clampLatitude(45.0), 0.0001);
    }

    @Test
    public void normalizeLongitude_wrapsPastDateline() {
        assertEquals(-170.0, GeoUtils.normalizeLongitude(190.0), 0.0001);
        assertEquals(170.0, GeoUtils.normalizeLongitude(-190.0), 0.0001);
        assertEquals(10.0, GeoUtils.normalizeLongitude(10.0), 0.0001);
    }

    @Test
    public void coordinateValidationAndMidpoints_workAsExpected() {
        assertTrue(GeoUtils.isValidCoordinate(10.0, 20.0));
        assertFalse(GeoUtils.isValidCoordinate(91.0, 20.0));
        assertFalse(GeoUtils.isValidCoordinate(10.0, 181.0));
        assertEquals(15.0, GeoUtils.midpointLatitude(10.0, 20.0), 0.0001);
        assertEquals(-180.0, GeoUtils.midpointLongitude(170.0, -170.0), 0.0001);
    }
}
