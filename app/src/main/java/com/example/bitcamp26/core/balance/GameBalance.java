package com.example.bitcamp26.core.balance;

import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for all game balance values.
 *
 * Key changes in this version:
 * - Shrink interval is fixed at 25 seconds (not map-radius-scaled)
 * - Location updates fire every 2.5 seconds = 10 pings per shrink interval
 * - The countdown shown to players is ping-based (10 → 1), not seconds-based
 * - Vision multipliers added for all role/powerup combinations
 */
public class GameBalance {

    public static final double DEFAULT_MAP_RADIUS_METERS = 150.0;

    // ── Shrink timing ─────────────────────────────────────────────────────────

    /**
     * Fixed shrink interval: 25 seconds.
     * At 2.5s per location ping, this equals exactly 10 pings per interval.
     */
    public static int shrinkIntervalSeconds() {
        return 25;
    }

    /**
     * Location update interval in milliseconds.
     * 2500ms = 2.5 seconds per ping.
     */
    public static int locationUpdateIntervalMs() {
        return 2500;
    }

    /**
     * Fastest acceptable location update interval in milliseconds.
     */
    public static int locationFastestIntervalMs() {
        return 1500;
    }

    /**
     * Number of location pings per shrink interval.
     * shrinkIntervalSeconds / (locationUpdateIntervalMs / 1000) = 25 / 2.5 = 10
     */
    public static int pingsPerShrinkInterval() {
        return shrinkIntervalSeconds() / (locationUpdateIntervalMs() / 1000);
        // = 25 / 2 = 10 (integer division is fine, matches exactly)
    }

    /**
     * The ping count at which to start showing the shrink warning countdown.
     * 10 means the countdown starts from 10 pings before each shrink.
     */
    public static int shrinkWarningPings() {
        return pingsPerShrinkInterval(); // warn from the very first ping = full 10-count
    }

    // ── Head start ────────────────────────────────────────────────────────────

    public static int headStartSeconds(double mapRadiusMeters) {
        return (int) Math.max(20, mapRadiusMeters / 7.0);
    }

    // ── Shrink amount ─────────────────────────────────────────────────────────

    public static double shrinkAmountMeters(double currentRadiusMeters) {
        return currentRadiusMeters * 0.20;
    }

    public static double minRadiusMeters() {
        return 20.0;
    }

    // ── Catch system ──────────────────────────────────────────────────────────

    public static double catchEligibilityRadiusMeters(double mapRadiusMeters) {
        return Math.max(15.0, mapRadiusMeters * 0.10);
    }

    // ── Hotspots ──────────────────────────────────────────────────────────────

    public static double hotspotRadiusMeters(double mapRadiusMeters) {
        return Math.max(10.0, mapRadiusMeters * 0.07);
    }

    public static int hotspotDwellSeconds() {
        return 5;
    }

    public static int hotspotCount(double mapRadiusMeters) {
        if (mapRadiusMeters < 100) return 3;
        if (mapRadiusMeters < 200) return 4;
        return 5;
    }

    // ── Powerup durations ─────────────────────────────────────────────────────

    public static int hiderInvisibilityDurationSeconds(double mapRadiusMeters) {
        return (int) Math.max(20, mapRadiusMeters / 8.0);
    }

    public static int seekerRevealAllDurationSeconds(double mapRadiusMeters) {
        return (int) Math.max(15, mapRadiusMeters / 10.0);
    }

    // ── Vision multipliers ────────────────────────────────────────────────────
    // These scale the effective "vision radius" — how far a player's map
    // view or marker visibility range extends relative to the base radius.

    /** Seeker default vision: 1x */
    public static final double SEEKER_VISION_MULTIPLIER         = 1.0;

    /** Hider default vision: 1.3x (hiders see slightly further) */
    public static final double HIDER_VISION_MULTIPLIER          = 1.3;

    /** Seeker with SEEKER_REVEAL_ALL active: 2x */
    public static final double SEEKER_BUFF_VISION_MULTIPLIER    = 2.0;

    /** Seeker while hider HIDER_INVISIBILITY is active: 0.7x */
    public static final double SEEKER_NERF_VISION_MULTIPLIER    = 0.7;

    // ── Start cluster ─────────────────────────────────────────────────────────

    public static double startClusterRadiusMeters() {
        return 30.0;
    }

    // ── Disconnect timeout ────────────────────────────────────────────────────

    public static int disconnectTimeoutSeconds() {
        return 60;
    }

    // ── Match time limit ──────────────────────────────────────────────────────

    public static int matchTimeLimitSeconds(double mapRadiusMeters) {
        return (int) Math.max(300, mapRadiusMeters * 3.0);
    }

    // ── Firebase config map ───────────────────────────────────────────────────

    public static Map<String, Object> defaultConfig(double mapRadiusMeters) {
        Map<String, Object> config = new HashMap<>();
        config.put("headStartSeconds",                  headStartSeconds(mapRadiusMeters));
        config.put("shrinkIntervalSeconds",             shrinkIntervalSeconds());
        config.put("locationUpdateIntervalMs",          locationUpdateIntervalMs());
        config.put("pingsPerShrinkInterval",            pingsPerShrinkInterval());
        config.put("catchEligibilityRadiusMeters",      catchEligibilityRadiusMeters(mapRadiusMeters));
        config.put("hotspotRadiusMeters",               hotspotRadiusMeters(mapRadiusMeters));
        config.put("hotspotDwellSeconds",               hotspotDwellSeconds());
        config.put("hiderInvisibilityDurationSeconds",  hiderInvisibilityDurationSeconds(mapRadiusMeters));
        config.put("seekerRevealAllDurationSeconds",    seekerRevealAllDurationSeconds(mapRadiusMeters));
        config.put("startClusterRadiusMeters",          startClusterRadiusMeters());
        config.put("disconnectTimeoutSeconds",          disconnectTimeoutSeconds());
        config.put("matchTimeLimitSeconds",             matchTimeLimitSeconds(mapRadiusMeters));
        config.put("seekerVisionMultiplier",            SEEKER_VISION_MULTIPLIER);
        config.put("hiderVisionMultiplier",             HIDER_VISION_MULTIPLIER);
        config.put("seekerBuffVisionMultiplier",        SEEKER_BUFF_VISION_MULTIPLIER);
        config.put("seekerNerfVisionMultiplier",        SEEKER_NERF_VISION_MULTIPLIER);
        return config;
    }
}
