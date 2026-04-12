package com.example.bitcamp26.core.balance;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GameBalanceTest {

    @Test
    public void headStartSeconds_respectsMinimumAndScaling() {
        assertEquals(30, GameBalance.headStartSeconds(100));
        assertEquals(60, GameBalance.headStartSeconds(300));
    }

    @Test
    public void shrinkIntervalSeconds_respectsMinimumAndScaling() {
        assertEquals(60, GameBalance.shrinkIntervalSeconds(50));
        assertEquals(160, GameBalance.shrinkIntervalSeconds(200));
    }

    @Test
    public void shrinkValues_areStable() {
        assertEquals(15, GameBalance.shrinkWarningSeconds());
        assertEquals(30.0, GameBalance.shrinkAmountMeters(150), 0.0001);
        assertEquals(20.0, GameBalance.minRadiusMeters(), 0.0001);
    }

    @Test
    public void catchAndHotspotSizing_haveExpectedFloors() {
        assertEquals(15.0, GameBalance.catchEligibilityRadiusMeters(50), 0.0001);
        assertEquals(25.0, GameBalance.catchEligibilityRadiusMeters(250), 0.0001);
        assertEquals(10.0, GameBalance.hotspotRadiusMeters(50), 0.0001);
        assertEquals(14.0, GameBalance.hotspotRadiusMeters(200), 0.0001);
    }

    @Test
    public void hotspotConfiguration_matchesThresholds() {
        assertEquals(5, GameBalance.hotspotDwellSeconds());
        assertEquals(3, GameBalance.hotspotCount(99));
        assertEquals(4, GameBalance.hotspotCount(150));
        assertEquals(5, GameBalance.hotspotCount(200));
    }

    @Test
    public void powerupDurations_scaleWithMapSize() {
        assertEquals(20, GameBalance.hiderInvisibilityDurationSeconds(80));
        assertEquals(25, GameBalance.hiderInvisibilityDurationSeconds(200));
        assertEquals(15, GameBalance.seekerRevealAllDurationSeconds(100));
        assertEquals(25, GameBalance.seekerRevealAllDurationSeconds(250));
    }

    @Test
    public void timingConstants_areStable() {
        assertEquals(2500, GameBalance.locationUpdateIntervalMs());
        assertEquals(1500, GameBalance.locationFastestIntervalMs());
        assertEquals(30.0, GameBalance.startClusterRadiusMeters(), 0.0001);
        assertEquals(60, GameBalance.disconnectTimeoutSeconds());
        assertEquals(300, GameBalance.matchTimeLimitSeconds(90));
        assertEquals(600, GameBalance.matchTimeLimitSeconds(200));
    }

    @Test
    public void defaultConfig_containsExpectedDerivedFields() {
        Map<String, Object> config = GameBalance.defaultConfig(150);

        assertEquals(GameBalance.headStartSeconds(150), config.get("headStartSeconds"));
        assertEquals(GameBalance.shrinkIntervalSeconds(150), config.get("shrinkIntervalSeconds"));
        assertEquals(GameBalance.shrinkWarningSeconds(), config.get("shrinkWarningSeconds"));
        assertEquals(GameBalance.catchEligibilityRadiusMeters(150), (Double) config.get("catchEligibilityRadiusMeters"), 0.0001);
        assertEquals(GameBalance.hotspotRadiusMeters(150), (Double) config.get("hotspotRadiusMeters"), 0.0001);
        assertEquals(GameBalance.hotspotDwellSeconds(), config.get("hotspotDwellSeconds"));
        assertEquals(GameBalance.hiderInvisibilityDurationSeconds(150), config.get("hiderInvisibilityDurationSeconds"));
        assertEquals(GameBalance.seekerRevealAllDurationSeconds(150), config.get("seekerRevealAllDurationSeconds"));
        assertEquals(GameBalance.startClusterRadiusMeters(), (Double) config.get("startClusterRadiusMeters"), 0.0001);
        assertEquals(GameBalance.disconnectTimeoutSeconds(), config.get("disconnectTimeoutSeconds"));
        assertEquals(GameBalance.matchTimeLimitSeconds(150), config.get("matchTimeLimitSeconds"));
        assertEquals(GameBalance.locationUpdateIntervalMs(), config.get("locationUpdateIntervalMs"));
        assertTrue(config.containsKey("locationUpdateIntervalMs"));
    }
}
