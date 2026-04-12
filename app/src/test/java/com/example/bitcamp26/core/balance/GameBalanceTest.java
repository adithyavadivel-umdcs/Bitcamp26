package com.example.bitcamp26.core.balance;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GameBalanceTest {

    @Test
    public void hotspotSizingAndCoverageAreFixed() {
        assertEquals(15.0, GameBalance.HOTSPOT_SIDE_LENGTH_FEET, 0.0001);
        assertEquals(GameBalance.HOTSPOT_SIDE_LENGTH_METERS / 2.0,
                GameBalance.hotspotRadiusMeters(50), 0.0001);
        assertEquals(GameBalance.HOTSPOT_SIDE_LENGTH_METERS / 2.0,
                GameBalance.hotspotRadiusMeters(300), 0.0001);
        assertTrue(GameBalance.hotspotCount(150) > 0);
    }

    @Test
    public void shrinkValuesMatchCurrentFlatRules() {
        assertEquals(120, GameBalance.shrinkIntervalSeconds(50));
        assertEquals(120, GameBalance.shrinkIntervalSeconds(300));
        assertEquals(15, GameBalance.shrinkWarningSeconds());
        assertEquals(15.0, GameBalance.shrinkAmountMeters(150), 0.0001);
        assertEquals(30.0, GameBalance.minRadiusMeters(150), 0.0001);
    }

    @Test
    public void powerupDurationsAndMultipliersMatchVisibilityModel() {
        assertEquals(20, GameBalance.hiderVisionReductionDurationSeconds(80));
        assertEquals(31, GameBalance.hiderVisionReductionDurationSeconds(250));
        assertEquals(15, GameBalance.seekerMinimapBoostDurationSeconds(100));
        assertEquals(25, GameBalance.seekerMinimapBoostDurationSeconds(250));
        assertEquals(2.0, GameBalance.seekerMinimapBoostMultiplier(), 0.0001);
        assertEquals(0.6, GameBalance.hiderVisionReductionMultiplier(), 0.0001);
    }

    @Test
    public void defaultConfigContainsCurrentDerivedFields() {
        Map<String, Object> config = GameBalance.defaultConfig(150);

        assertEquals(GameBalance.headStartSeconds(150), config.get("headStartSeconds"));
        assertEquals(GameBalance.shrinkIntervalSeconds(150), config.get("shrinkIntervalSeconds"));
        assertEquals(GameBalance.hotspotDwellSeconds(), config.get("hotspotDwellSeconds"));
        assertEquals(GameBalance.hotspotPersonalCooldownSeconds(), config.get("hotspotPersonalCooldownSeconds"));
        assertEquals(GameBalance.HOTSPOT_SIDE_LENGTH_METERS,
                (Double) config.get("hotspotSideLengthMeters"), 0.0001);
        assertEquals(GameBalance.hiderVisionReductionMultiplier(),
                (Double) config.get("hiderVisionReductionMultiplier"), 0.0001);
        assertEquals(GameBalance.seekerMinimapBoostMultiplier(),
                (Double) config.get("seekerMinimapBoostMultiplier"), 0.0001);
    }
}
