package com.example.bitcamp26.core.util;

import androidx.annotation.NonNull;

import com.example.bitcamp26.core.balance.GameBalance;

/**
 * Manages the ping-based shrink countdown.
 *
 * The shrink interval is 25 seconds. Location updates fire every 2.5 seconds.
 * That gives exactly 10 pings per shrink interval.
 *
 * Each time a location ping fires, call {@link #onLocationPing()}.
 * The manager tracks how many pings remain until the next shrink and
 * fires the appropriate callbacks.
 *
 * Countdown display: 10 → 9 → 8 → ... → 1 → SHRINK
 *
 * Usage:
 *   ShrinkCountdownManager manager = new ShrinkCountdownManager(listener);
 *   manager.start();
 *   // call manager.onLocationPing() from your location update loop
 *   manager.stop();
 */
public class ShrinkCountdownManager {

    public interface ShrinkListener {
        /**
         * Called every ping while the countdown is active.
         * @param pingsRemaining number of pings left until shrink (10 down to 1)
         */
        void onCountdownTick(int pingsRemaining);

        /**
         * Called when the countdown reaches zero — the zone should shrink now.
         * After this fires the countdown resets to pingsPerShrinkInterval.
         */
        void onShrinkNow();
    }

    private final ShrinkListener listener;
    private final int totalPings;

    private int pingsRemaining;
    private boolean running;

    public ShrinkCountdownManager(@NonNull ShrinkListener listener) {
        this.listener      = listener;
        this.totalPings    = GameBalance.pingsPerShrinkInterval(); // 10
        this.pingsRemaining = totalPings;
        this.running       = false;
    }

    /** Start accepting pings. Resets the countdown to full. */
    public void start() {
        pingsRemaining = totalPings;
        running        = true;
    }

    /** Stop the countdown. Pending pings are ignored until start() is called again. */
    public void stop() {
        running = false;
    }

    /** Reset the countdown back to full without stopping. */
    public void reset() {
        pingsRemaining = totalPings;
    }

    /**
     * Call this every time a location ping fires (every 2.5 seconds).
     * Decrements the countdown and fires the appropriate listener callback.
     */
    public void onLocationPing() {
        if (!running) return;

        pingsRemaining--;

        if (pingsRemaining <= 0) {
            // Shrink fires
            pingsRemaining = totalPings;
            listener.onShrinkNow();
        } else {
            listener.onCountdownTick(pingsRemaining);
        }
    }

    /** Returns the number of pings remaining until the next shrink (1–10). */
    public int getPingsRemaining() {
        return pingsRemaining;
    }

    /** Returns the total number of pings per shrink interval (always 10). */
    public int getTotalPings() {
        return totalPings;
    }

    /** Returns true if the countdown is currently running. */
    public boolean isRunning() {
        return running;
    }

    /**
     * Convenience: returns the display string for the current countdown.
     * Returns "10", "9", ..., "1" — use this directly in your UI text view.
     */
    @NonNull
    public String getCountdownDisplayText() {
        return String.valueOf(Math.max(1, pingsRemaining));
    }

    /**
     * Returns true if the countdown is in the warning zone (≤ warningThreshold pings left).
     * Use this to switch the ShrinkBannerView to WARNING state.
     */
    public boolean isInWarningZone(int warningThreshold) {
        return pingsRemaining <= warningThreshold;
    }
}
