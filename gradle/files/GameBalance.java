package com.example.bitcamp26.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for all game balance values.
 * All formulas scale with mapRadiusMeters so the game feels right
 * whether you're playing in a parking lot or a large park.
 *
 * Default map radius: 150 meters (good for a hackathon demo outdoors).
 */
public class GameBalance {

    public static final double DEFAULT_MAP_RADIUS_METERS = 150.0;

    // ── Head start ──────────────────────────────────────────────────────────
    /** How long hiders get to run before seeker can move. Honor-based, UI countdown only. */
    public static int headStartSeconds(double mapRadiusMeters) {
        // ~30s base, scales up for larger maps
        return (int) Math.max(30, mapRadiusMeters / 5.0);
    }

    // ── Shrinking circle ────────────────────────────────────────────────────
    /** How often the circle shrinks (seconds) */
    public static int shrinkIntervalSeconds(double mapRadiusMeters) {
        return (int) Math.max(60, mapRadiusMeters * 0.8);
    }

    /** Warning shown before shrink (seconds before shrink fires) */
    public static int shrinkWarningSeconds() {
        return 15;
    }

    /** How much the radius shrinks each interval (meters) */
    public static double shrinkAmountMeters(double mapRadiusMeters) {
        return mapRadiusMeters * 0.20; // shrinks 20% of CURRENT radius each time
    }

    /** Minimum radius before the circle stops shrinking */
    public static double minRadiusMeters() {
        return 20.0;
    }

    // ── Catch system ────────────────────────────────────────────────────────
    /** Seeker must be within this radius of a hider to submit a catch code */
    public static double catchEligibilityRadiusMeters(double mapRadiusMeters) {
        return Math.max(15.0, mapRadiusMeters * 0.10);
    }

    // ── Hotspots ─────────────────────────────────────────────────────────────
    /** Radius of each hotspot claim circle */
    public static double hotspotRadiusMeters(double mapRadiusMeters) {
        return Math.max(10.0, mapRadiusMeters * 0.07);
    }

    /** How long a player must stand in hotspot to claim it (seconds) */
    public static int hotspotDwellSeconds() {
        return 5;
    }

    /** Number of hotspots on the map */
    public static int hotspotCount(double mapRadiusMeters) {
        if (mapRadiusMeters < 100) return 3;
        if (mapRadiusMeters < 200) return 4;
        return 5;
    }

    // ── Powerup durations ────────────────────────────────────────────────────
    /** How long hider invisibility lasts (seconds) */
    public static int hiderInvisibilityDurationSeconds(double mapRadiusMeters) {
        return (int) Math.max(20, mapRadiusMeters / 8.0);
    }

    /** How long seeker reveal-all lasts (seconds) */
    public static int seekerRevealAllDurationSeconds(double mapRadiusMeters) {
        return (int) Math.max(15, mapRadiusMeters / 10.0);
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
        config.put("hotspotDwellSeconds", hotspotDwellSeconds());
        config.put("hiderInvisibilityDurationSeconds", hiderInvisibilityDurationSeconds(mapRadiusMeters));
        config.put("seekerRevealAllDurationSeconds", seekerRevealAllDurationSeconds(mapRadiusMeters));
        config.put("startClusterRadiusMeters", startClusterRadiusMeters());
        config.put("disconnectTimeoutSeconds", disconnectTimeoutSeconds());
        config.put("matchTimeLimitSeconds", matchTimeLimitSeconds(mapRadiusMeters));
        config.put("locationUpdateIntervalMs", locationUpdateIntervalMs());
        return config;
    }
}
