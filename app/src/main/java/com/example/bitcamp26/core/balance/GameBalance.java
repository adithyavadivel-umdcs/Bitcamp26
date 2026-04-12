package com.example.bitcamp26.core.balance;
import java.util.HashMap;
import java.util.Map;

public class GameBalance {

    public static final double DEFAULT_MAP_RADIUS_METERS = 150.0;
    public static final double HOTSPOT_SIDE_LENGTH_FEET = 15.0;
    public static final double HOTSPOT_SIDE_LENGTH_METERS = HOTSPOT_SIDE_LENGTH_FEET * 0.3048;
    public static final double HOTSPOT_HALF_WIDTH_METERS = HOTSPOT_SIDE_LENGTH_METERS / 2.0;
    public static final double HOTSPOT_MAX_COVERAGE_FRACTION = 0.10;

    // ── Head start ──────────────────────────────────────────────────────────
    /** How long hiders get to run before seeker can move. Honor-based, UI countdown only. */
    public static int headStartSeconds(double mapRadiusMeters) {
        // ~30s base, scales up for larger maps
        return (int) Math.max(30, mapRadiusMeters / 5.0);
    }

    // ── Shrinking circle ────────────────────────────────────────────────────
    /** How often the circle shrinks (seconds) */
    public static int shrinkIntervalSeconds(double mapRadiusMeters) {
        return 120;
    }

    /** Warning shown before shrink (seconds before shrink fires) */
    public static int shrinkWarningSeconds() {
        return 15;
    }

    /** How much the radius shrinks each interval (meters) */
    public static double shrinkAmountMeters(double mapRadiusMeters) {
        return mapRadiusMeters * 0.10;
    }

    /** Minimum radius before the circle stops shrinking */
    public static double minRadiusMeters(double mapRadiusMeters) {
        return mapRadiusMeters * 0.20;
    }

    // ── Catch system ────────────────────────────────────────────────────────
    /** Seeker must be within this radius of a hider to submit a catch code */
    public static double catchEligibilityRadiusMeters(double mapRadiusMeters) {
        return Math.max(15.0, mapRadiusMeters * 0.10);
    }

    // ── Hotspots ─────────────────────────────────────────────────────────────
    /** Stored hotspot half-width for a fixed 15ft by 15ft square hotspot. */
    public static double hotspotRadiusMeters(double mapRadiusMeters) {
        return HOTSPOT_HALF_WIDTH_METERS;
    }

    /** How long a player must stand in hotspot to claim it (seconds) */
    public static int hotspotDwellSeconds() {
        return 15;
    }

    /** Personal cooldown after a hotspot reward before another hotspot can grant again (seconds). */
    public static int hotspotPersonalCooldownSeconds() {
        return 120;
    }

    /** Number of hotspots on the map */
    public static int hotspotCount(double mapRadiusMeters) {
        double mapArea = Math.pow(mapRadiusMeters * 2.0, 2.0);
        double hotspotArea = Math.pow(HOTSPOT_SIDE_LENGTH_METERS, 2.0);
        double rawLimit = (mapArea * HOTSPOT_MAX_COVERAGE_FRACTION) / hotspotArea;
        if (rawLimit <= 0.0) {
            return 0;
        }

        int maxCount = Math.floor(rawLimit) == rawLimit
                ? (int) rawLimit - 1
                : (int) Math.floor(rawLimit);
        return Math.max(0, maxCount);
    }

    // ── Powerup durations ────────────────────────────────────────────────────
    /** How long the hider-specific seeker vision reduction lasts (seconds). */
    public static int hiderVisionReductionDurationSeconds(double mapRadiusMeters) {
        return (int) Math.max(20, mapRadiusMeters / 8.0);
    }

    /** How long the seeker minimap boost lasts (seconds). */
    public static int seekerMinimapBoostDurationSeconds(double mapRadiusMeters) {
        return (int) Math.max(15, mapRadiusMeters / 10.0);
    }

    /** Seeker minimap width and height multiplier while the seeker powerup is active. */
    public static double seekerMinimapBoostMultiplier() {
        return 2.0;
    }

    /** Seeker-specific visibility multiplier applied to a hider while their powerup is active. */
    public static double hiderVisionReductionMultiplier() {
        return 0.6;
    }

    /** Legacy compatibility wrapper. */
    @Deprecated
    public static int hiderInvisibilityDurationSeconds(double mapRadiusMeters) {
        return hiderVisionReductionDurationSeconds(mapRadiusMeters);
    }

    /** Legacy compatibility wrapper. */
    @Deprecated
    public static int seekerRevealAllDurationSeconds(double mapRadiusMeters) {
        return seekerMinimapBoostDurationSeconds(mapRadiusMeters);
    }

    // ── Location update rate ─────────────────────────────────────────────────
    /** How often the client sends location to Firebase (milliseconds) */
    public static int locationUpdateIntervalMs() {
        return 2500;
    }

    /** Fastest acceptable location update (milliseconds) */
    public static int locationFastestIntervalMs() {
        return 1500;
    }

    // ── Start cluster ────────────────────────────────────────────────────────
    /** All players must be within this radius of host before game can start */
    public static double startClusterRadiusMeters() {
        return 30.0;
    }

    // ── Disconnect timeout ───────────────────────────────────────────────────
    /** Player eliminated if disconnected this long during active match (seconds) */
    public static int disconnectTimeoutSeconds() {
        return 60;
    }

    // ── Match time limit ─────────────────────────────────────────────────────
    /** Total match duration in seconds before surviving hiders auto-win */
    public static int matchTimeLimitSeconds(double mapRadiusMeters) {
        return (int) Math.max(300, mapRadiusMeters * 3.0);
    }

    // ── Helper: default config map for Firebase ───────────────────────────────
    public static Map<String, Object> defaultConfig(double mapRadiusMeters) {
        Map<String, Object> config = new HashMap<>();
        config.put("headStartSeconds", headStartSeconds(mapRadiusMeters));
        config.put("shrinkIntervalSeconds", shrinkIntervalSeconds(mapRadiusMeters));
        config.put("shrinkWarningSeconds", shrinkWarningSeconds());
        config.put("catchEligibilityRadiusMeters", catchEligibilityRadiusMeters(mapRadiusMeters));
        config.put("hotspotRadiusMeters", hotspotRadiusMeters(mapRadiusMeters));
        config.put("hotspotSideLengthMeters", HOTSPOT_SIDE_LENGTH_METERS);
        config.put("hotspotMaxCoverageFraction", HOTSPOT_MAX_COVERAGE_FRACTION);
        config.put("hotspotDwellSeconds", hotspotDwellSeconds());
        config.put("hotspotPersonalCooldownSeconds", hotspotPersonalCooldownSeconds());
        config.put("hiderVisionReductionDurationSeconds", hiderVisionReductionDurationSeconds(mapRadiusMeters));
        config.put("seekerMinimapBoostDurationSeconds", seekerMinimapBoostDurationSeconds(mapRadiusMeters));
        config.put("hiderVisionReductionMultiplier", hiderVisionReductionMultiplier());
        config.put("seekerMinimapBoostMultiplier", seekerMinimapBoostMultiplier());
        config.put("startClusterRadiusMeters", startClusterRadiusMeters());
        config.put("disconnectTimeoutSeconds", disconnectTimeoutSeconds());
        config.put("matchTimeLimitSeconds", matchTimeLimitSeconds(mapRadiusMeters));
        config.put("locationUpdateIntervalMs", locationUpdateIntervalMs());
        return config;
    }
}
