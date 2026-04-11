package com.example.bitcamp26.core.util;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Time-related utility methods for countdowns, durations, and timestamps.
 */
public final class TimeUitls {

    private TimeUitls() {
        // Utility class
    }

    /**
     * Returns the current system time in milliseconds.
     */
    public static long nowMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Returns true if the current time is at or past the given timestamp.
     */
    public static boolean hasElapsed(long targetTimestampMillis) {
        return nowMillis() >= targetTimestampMillis;
    }

    /**
     * Returns the non-negative number of milliseconds remaining until the target time.
     */
    public static long millisRemaining(long targetTimestampMillis) {
        return Math.max(0L, targetTimestampMillis - nowMillis());
    }

    /**
     * Returns the non-negative number of seconds remaining until the target time.
     */
    public static long secondsRemaining(long targetTimestampMillis) {
        return TimeUnit.MILLISECONDS.toSeconds(millisRemaining(targetTimestampMillis));
    }

    /**
     * Formats a duration in milliseconds as mm:ss.
     * Example: 65000 -> 01:05
     */
    public static String formatMinutesSeconds(long durationMillis) {
        long totalSeconds = Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(durationMillis));
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    /**
     * Formats the remaining time until a timestamp as mm:ss.
     */
    public static String formatRemainingMinutesSeconds(long targetTimestampMillis) {
        return formatMinutesSeconds(millisRemaining(targetTimestampMillis));
    }

    /**
     * Formats a duration as a human-readable string.
     * Examples: "45s", "3m 12s", "1h 05m"
     */
    public static String formatDurationHuman(long durationMillis) {
        long totalSeconds = Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(durationMillis));
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.US, "%dh %02dm", hours, minutes);
        }
        if (minutes > 0) {
            return String.format(Locale.US, "%dm %02ds", minutes, seconds);
        }
        return String.format(Locale.US, "%ds", seconds);
    }

    /**
     * Adds the given number of seconds to the current time and returns the future timestamp.
     */
    public static long secondsFromNow(long seconds) {
        return nowMillis() + TimeUnit.SECONDS.toMillis(seconds);
    }

    /**
     * Adds the given number of minutes to the current time and returns the future timestamp.
     */
    public static long minutesFromNow(long minutes) {
        return nowMillis() + TimeUnit.MINUTES.toMillis(minutes);
    }

    /**
     * Returns true if the timestamp is valid (greater than zero).
     */
    public static boolean isValidTimestamp(long timestampMillis) {
        return timestampMillis > 0L;
    }
}
